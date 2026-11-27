package com.example.casinoproject.service;

import com.example.casinoproject.domain.*;
import com.example.casinoproject.dto.DepositRequest;
import com.example.casinoproject.dto.PaymentResponse;
import com.example.casinoproject.dto.SavedCard;
import com.example.casinoproject.dto.WithdrawRequest;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.exception.PaymentFailedException;
import com.example.casinoproject.payment.CardValidator;
import com.example.casinoproject.payment.GatewayResponse;
import com.example.casinoproject.payment.GatewayUnavailableException;
import com.example.casinoproject.payment.PaymentGateway;
import com.example.casinoproject.repository.PaymentRepository;
import com.example.casinoproject.repository.UserAccountRepository;
import com.example.casinoproject.security.CryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Поповнення і виведення через псевдо-шлюз. Номер картки шифрується AES-GCM перед збереженням,
 * CVV не зберігається ніде, а кожна відповідь шлюзу перевіряється по HMAC-підпису.
 */
@Service
public class PaymentService {

    private final UserAccountRepository users;
    private final PaymentRepository payments;
    private final WalletService wallet;
    private final PaymentGateway gateway;
    private final CryptoService crypto;
    private final String gatewaySecret;

    public PaymentService(UserAccountRepository users, PaymentRepository payments, WalletService wallet,
                          PaymentGateway gateway, CryptoService crypto,
                          @Value("${casino.payment.hmac-secret}") String gatewaySecret) {
        this.users = users;
        this.payments = payments;
        this.wallet = wallet;
        this.gateway = gateway;
        this.crypto = crypto;
        this.gatewaySecret = gatewaySecret;
    }

    /**
     * Поповнення балансу. Відмова шлюзу не вважається помилкою програми: платіж зберігається зі статусом FAILED
     * і причиною, а баланс не змінюється. Подивитись результат можна в полі status відповіді.
     */
    @Transactional
    public PaymentResponse deposit(Long userId, DepositRequest req) {
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.DOWN);
        UserAccount user = lockActiveUser(userId);
        String card = resolveCard(userId, req.getCardNumber(), req.getSavedPaymentId());
        if (req.getSavedPaymentId() == null) {
            if (req.getExpMonth() == null || req.getExpYear() == null
                    || !CardValidator.isNotExpired(req.getExpMonth(), req.getExpYear())) {
                throw CasinoException.badRequest("Термін дії картки вказано невірно або він минув");
            }
        }

        Payment payment = newPayment(user, PaymentType.DEPOSIT, amount, card);
        GatewayResponse response;
        try {
            response = gateway.charge(card,
                    req.getExpMonth() == null ? 0 : req.getExpMonth(),
                    req.getExpYear() == null ? 0 : req.getExpYear(),
                    req.getCvv(), amount);
        } catch (GatewayUnavailableException e) {
            return fail(payment, e.getMessage());
        }
        if (!verified(response)) {
            return fail(payment, "Підпис відповіді шлюзу не збігся");
        }
        if (!response.approved()) {
            return fail(payment, response.message());
        }
        wallet.credit(user, amount, TransactionType.DEPOSIT, null);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderRef(response.reference());
        return PaymentResponse.from(payment);
    }

    /**
     * Виведення коштів. Тут навмисно все в одній транзакції: спочатку гроші списуються з балансу,
     * потім іде запит у шлюз, і якщо шлюз відмовив або впав, кидається PaymentFailedException
     * і списання разом із записом платежу відкочується. Так ми демонструємо відкат транзакції.
     */
    @Transactional(rollbackFor = PaymentFailedException.class)
    public PaymentResponse withdraw(Long userId, WithdrawRequest req) {
        BigDecimal amount = req.getAmount().setScale(2, RoundingMode.DOWN);
        UserAccount user = lockActiveUser(userId);
        String card = resolveCard(userId, req.getCardNumber(), req.getSavedPaymentId());

        wallet.debit(user, amount, TransactionType.WITHDRAWAL, null);
        Payment payment = newPayment(user, PaymentType.WITHDRAWAL, amount, card);

        GatewayResponse response;
        try {
            response = gateway.payout(card, amount);
        } catch (GatewayUnavailableException e) {
            throw new PaymentFailedException(e.getMessage());
        }
        if (!verified(response)) {
            throw new PaymentFailedException("Підпис відповіді шлюзу не збігся");
        }
        if (!response.approved()) {
            throw new PaymentFailedException(response.message());
        }
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setProviderRef(response.reference());
        return PaymentResponse.from(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> history(Long userId) {
        return payments.findTop20ByUserIdOrderByCreatedAtDesc(userId).stream().map(PaymentResponse::from).toList();
    }

    /**
     * Картки, якими гравець вже успішно поповнював рахунок. Повертаємо тільки маски і id платежу,
     * повний номер розшифровується лише в момент нової оплати.
     */
    @Transactional(readOnly = true)
    public List<SavedCard> savedCards(Long userId) {
        Map<String, SavedCard> byMask = new LinkedHashMap<>();
        for (Payment p : payments.findByUserStatusAndType(userId, PaymentStatus.SUCCESS, PaymentType.DEPOSIT)) {
            byMask.putIfAbsent(p.getCardMask(), new SavedCard(p.getId(), p.getCardMask()));
        }
        return List.copyOf(byMask.values());
    }

    private UserAccount lockActiveUser(Long userId) {
        UserAccount user = users.lockById(userId).orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
        if (user.isBlocked()) {
            throw CasinoException.forbidden("Акаунт заблоковано");
        }
        return user;
    }

    private String resolveCard(Long userId, String rawNumber, Long savedPaymentId) {
        if (savedPaymentId != null) {
            Payment saved = payments.findByIdAndUserIdAndStatus(savedPaymentId, userId, PaymentStatus.SUCCESS)
                    .orElseThrow(() -> CasinoException.notFound("Збережену картку не знайдено"));
            return crypto.decrypt(saved.getCardEncrypted());
        }
        String digits = CardValidator.normalize(rawNumber);
        if (!CardValidator.isValidNumber(digits)) {
            throw CasinoException.badRequest("Номер картки невірний");
        }
        return digits;
    }

    private Payment newPayment(UserAccount user, PaymentType type, BigDecimal amount, String card) {
        Payment p = new Payment();
        p.setUser(user);
        p.setType(type);
        p.setAmount(amount);
        p.setCardMask(CryptoService.maskCard(card));
        p.setCardEncrypted(crypto.encrypt(card));
        return payments.save(p);
    }

    private PaymentResponse fail(Payment payment, String message) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setErrorMessage(message);
        return PaymentResponse.from(payment);
    }

    private boolean verified(GatewayResponse r) {
        String expected = crypto.hmac(gatewaySecret, PaymentGateway.signaturePayload(r));
        return crypto.safeEquals(expected, r.signature());
    }
}

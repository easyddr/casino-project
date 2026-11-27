package com.example.casinoproject.payment;

import com.example.casinoproject.security.CryptoService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Псевдо-платіжна система. Гроші нікуди не йдуть, але поведінка схожа на справжній шлюз:
 * є відмови, збої і підпис відповіді. Тестові картки такі: 4242 4242 4242 4242 завжди успішна,
 * будь-яка картка що закінчується на 0002 отримує "недостатньо коштів", на 0119 шлюз "падає".
 * Сума більша за casino.payment.max-single-amount відхиляється як перевищення ліміту.
 */
@Component
public class FakePaymentGateway implements PaymentGateway {

    private final CryptoService crypto;
    private final String secret;
    private final BigDecimal maxSingleAmount;

    public FakePaymentGateway(CryptoService crypto,
                              @Value("${casino.payment.hmac-secret}") String secret,
                              @Value("${casino.payment.max-single-amount}") BigDecimal maxSingleAmount) {
        this.crypto = crypto;
        this.secret = secret;
        this.maxSingleAmount = maxSingleAmount;
    }

    /**
     * Списання з картки гравця (поповнення балансу казино).
     */
    @Override
    public GatewayResponse charge(String cardNumber, int expMonth, int expYear, String cvv, BigDecimal amount) {
        simulateNetwork(cardNumber);
        if (cvv == null || !cvv.matches("\\d{3,4}")) {
            return signed(false, null, amount, "Невірний CVV");
        }
        if (cardNumber.endsWith("0002")) {
            return signed(false, null, amount, "Недостатньо коштів на картці");
        }
        if (amount.compareTo(maxSingleAmount) > 0) {
            return signed(false, null, amount, "Перевищено ліміт однієї операції");
        }
        return signed(true, "PSP-CH-" + UUID.randomUUID(), amount, "OK");
    }

    /**
     * Виплата на картку гравця (виведення коштів).
     */
    @Override
    public GatewayResponse payout(String cardNumber, BigDecimal amount) {
        simulateNetwork(cardNumber);
        if (amount.compareTo(maxSingleAmount) > 0) {
            return signed(false, null, amount, "Перевищено ліміт однієї операції");
        }
        return signed(true, "PSP-PO-" + UUID.randomUUID(), amount, "OK");
    }

    private void simulateNetwork(String cardNumber) {
        try {
            Thread.sleep(150);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (cardNumber.endsWith("0119")) {
            throw new GatewayUnavailableException("Платіжний шлюз не відповідає, спробуйте пізніше");
        }
    }

    private GatewayResponse signed(boolean approved, String ref, BigDecimal amount, String message) {
        GatewayResponse unsigned = new GatewayResponse(approved, ref, amount, message, null);
        String signature = crypto.hmac(secret, PaymentGateway.signaturePayload(unsigned));
        return new GatewayResponse(approved, ref, amount, message, signature);
    }
}

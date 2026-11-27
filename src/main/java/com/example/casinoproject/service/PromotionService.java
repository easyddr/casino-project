package com.example.casinoproject.service;

import com.example.casinoproject.dao.HouseBankDao;
import com.example.casinoproject.dao.Promotion;
import com.example.casinoproject.dao.PromotionDao;
import com.example.casinoproject.domain.TransactionType;
import com.example.casinoproject.domain.UserAccount;
import com.example.casinoproject.dto.PromotionRequest;
import com.example.casinoproject.exception.CasinoException;
import com.example.casinoproject.repository.UserAccountRepository;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
public class PromotionService {

    private final PromotionDao promotions;
    private final UserAccountRepository users;
    private final WalletService wallet;
    private final HouseBankDao bank;

    public PromotionService(PromotionDao promotions, UserAccountRepository users, WalletService wallet, HouseBankDao bank) {
        this.promotions = promotions;
        this.users = users;
        this.wallet = wallet;
        this.bank = bank;
    }

    /**
     * Активує промокод для гравця. Транзакційний метод, тут кілька кроків, які мають пройти разом:
     * позначка в user_promotions, нарахування бонусу гравцю і списання бонусу з банку казино.
     * Якщо код уже використаний, недійсний або банк не може оплатити бонус, нічого не зміниться.
     */
    @Transactional
    public BigDecimal activate(Long userId, String code) {
        Promotion promo = promotions.findByCode(code)
                .orElseThrow(() -> CasinoException.notFound("Промокод не знайдено"));
        if (!promo.isUsableAt(OffsetDateTime.now())) {
            throw CasinoException.conflict("Промокод неактивний або прострочений");
        }
        UserAccount user = users.lockById(userId).orElseThrow(() -> CasinoException.notFound("Користувача не знайдено"));
        if (user.isBlocked()) {
            throw CasinoException.forbidden("Акаунт заблоковано");
        }
        if (promotions.isActivatedBy(userId, promo.id())) {
            throw CasinoException.conflict("Ви вже використали цей промокод");
        }
        promotions.markActivated(userId, promo.id());
        BigDecimal balance = wallet.credit(user, promo.bonusAmount(), TransactionType.BONUS, null);
        bank.lockBalance();
        bank.add(promo.bonusAmount().negate());
        return balance;
    }

    public List<Promotion> active() {
        return promotions.findActive();
    }

    public List<Promotion> all() {
        return promotions.findAll();
    }

    public Promotion get(long id) {
        return promotions.findById(id).orElseThrow(() -> CasinoException.notFound("Промокод не знайдено"));
    }

    /**
     * Створює промокод. Код має бути унікальним, інакше 409.
     */
    public Promotion create(PromotionRequest r) {
        try {
            long id = promotions.create(new Promotion(null, r.getCode(), r.getTitle(), r.getBonusAmount(),
                    r.getValidUntil(), r.isActive()));
            return get(id);
        } catch (DuplicateKeyException e) {
            throw CasinoException.conflict("Такий промокод вже існує");
        }
    }

    public Promotion update(long id, PromotionRequest r) {
        get(id);
        try {
            promotions.update(new Promotion(id, r.getCode(), r.getTitle(), r.getBonusAmount(), r.getValidUntil(), r.isActive()));
        } catch (DuplicateKeyException e) {
            throw CasinoException.conflict("Такий промокод вже існує");
        }
        return get(id);
    }

    /**
     * Видаляє промокод разом з історією активацій (ON DELETE CASCADE у user_promotions). Бонуси, які вже нараховані, лишаються.
     */
    public void delete(long id) {
        if (promotions.delete(id) == 0) {
            throw CasinoException.notFound("Промокод не знайдено");
        }
    }
}

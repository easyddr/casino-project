package com.example.casinoproject.game;

import com.example.casinoproject.exception.CasinoException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Кубик від 0.00 до 99.99. Гравець обирає шанс у відсотках (2..95) і виграє, якщо випало менше за шанс.
 * Множник рахується як 96 / шанс, тобто казино тримає 4% з кожної ставки незалежно від обраного ризику.
 */
@Component
public class DiceGame {

    private static final Logger log = LoggerFactory.getLogger(DiceGame.class);

    public static final int MIN_CHANCE = 2;
    public static final int MAX_CHANCE = 95;
    private static final BigDecimal RTP_PERCENT = new BigDecimal("96");

    private final SecureRandom random;

    public DiceGame(SecureRandom random) {
        this.random = random;
    }

    /**
     * Множник для заданого шансу, наприклад для 50% це 1.92. Можна викликати з фронту для підказки.
     */
    public static BigDecimal multiplierFor(int chance) {
        return RTP_PERCENT.divide(BigDecimal.valueOf(chance), 4, RoundingMode.DOWN);
    }

    /**
     * Кидає кубик. Якщо гравець виграв, але чистий виграш виходить за maxWin, кидок замінюється
     * на випадкове число з програшної зони.
     */
    public GameOutcome roll(BigDecimal amount, int chance, BigDecimal maxWin) {
        if (chance < MIN_CHANCE || chance > MAX_CHANCE) {
            throw CasinoException.badRequest("Шанс має бути від " + MIN_CHANCE + " до " + MAX_CHANCE);
        }
        int threshold = chance * 100;
        int roll = random.nextInt(10_000);
        BigDecimal multiplier = multiplierFor(chance);
        boolean win = roll < threshold;
        BigDecimal payout = win ? amount.multiply(multiplier).setScale(2, RoundingMode.DOWN) : BigDecimal.ZERO;

        if (win && payout.subtract(amount).compareTo(maxWin) > 0) {
            log.info("Кубик: виграш {} більший за ліміт {}, кидок замінено", payout, maxWin);
            roll = threshold + random.nextInt(10_000 - threshold);
            win = false;
            payout = BigDecimal.ZERO;
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("roll", BigDecimal.valueOf(roll, 2));
        details.put("chance", chance);
        details.put("multiplier", multiplier);
        details.put("win", win);
        return new GameOutcome(payout.setScale(2, RoundingMode.DOWN), win ? multiplier : BigDecimal.ZERO, details);
    }
}

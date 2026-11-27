package com.example.casinoproject.game;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Слот на три барабани. Символи мають різну вагу, тому вишні випадають часто, а сімки рідко.
 * Три однакові платять множник символу, дві вишні платять 1.5. Теоретична віддача приблизно 87%,
 * тобто на дистанції казино забирає близько 13% від ставок.
 */
@Component
public class SlotMachine {

    private static final Logger log = LoggerFactory.getLogger(SlotMachine.class);

    public enum Symbol {
        CHERRY(30, "5"), LEMON(25, "9"), BELL(18, "18"), CLOVER(12, "35"), DIAMOND(7, "90"), SEVEN(3, "300");

        final int weight;
        final BigDecimal threeOfKind;

        Symbol(int weight, String threeOfKind) {
            this.weight = weight;
            this.threeOfKind = new BigDecimal(threeOfKind);
        }
    }

    private static final BigDecimal TWO_CHERRIES = new BigDecimal("1.5");
    private static final int TOTAL_WEIGHT = Arrays.stream(Symbol.values()).mapToInt(s -> s.weight).sum();

    private final SecureRandom random;

    public SlotMachine(SecureRandom random) {
        this.random = random;
    }

    /**
     * Крутить барабани для ставки amount. Якщо чистий виграш більший за maxWin (ліміт банку),
     * результат тихо перекручується, поки не випаде програшна комбінація.
     */
    public GameOutcome spin(BigDecimal amount, BigDecimal maxWin) {
        Symbol[] reels = roll();
        BigDecimal multiplier = multiplier(reels);
        BigDecimal payout = amount.multiply(multiplier).setScale(2, RoundingMode.DOWN);

        if (payout.subtract(amount).compareTo(maxWin) > 0) {
            log.info("Слоти: виграш {} більший за ліміт {}, результат замінено", payout, maxWin);
            do {
                reels = roll();
                multiplier = multiplier(reels);
            } while (multiplier.signum() > 0);
            payout = BigDecimal.ZERO.setScale(2);
        }

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("reels", Arrays.stream(reels).map(Enum::name).toList());
        details.put("multiplier", multiplier);
        return new GameOutcome(payout, multiplier, details);
    }

    /**
     * Таблиця виплат для показу на сторінці гри.
     */
    public List<Map<String, Object>> paytable() {
        return Arrays.stream(Symbol.values())
                .map(s -> Map.<String, Object>of("symbol", s.name(), "multiplier", s.threeOfKind))
                .toList();
    }

    private Symbol[] roll() {
        return new Symbol[]{pick(), pick(), pick()};
    }

    private Symbol pick() {
        int r = random.nextInt(TOTAL_WEIGHT);
        for (Symbol s : Symbol.values()) {
            r -= s.weight;
            if (r < 0) {
                return s;
            }
        }
        return Symbol.CHERRY;
    }

    private BigDecimal multiplier(Symbol[] reels) {
        if (reels[0] == reels[1] && reels[1] == reels[2]) {
            return reels[0].threeOfKind;
        }
        long cherries = Arrays.stream(reels).filter(s -> s == Symbol.CHERRY).count();
        return cherries == 2 ? TWO_CHERRIES : BigDecimal.ZERO;
    }
}

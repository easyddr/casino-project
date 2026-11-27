package com.example.casinoproject.game;

import java.math.BigDecimal;
import java.security.SecureRandom;

/**
 * Генерує весь графік ціни монети для одного крипто-раунду наперед. Ціна рухається випадково
 * з невеликим нахилом вниз, а на кожному тіку є шанс обвалу (rug pull), після якого ставка згорає.
 * Бін має scope prototype, бо тримає поточну ціну раунду, отримувати його треба через ObjectProvider.
 */
public class PricePathGenerator {

    public static final double SELL_FEE = 0.97;
    private static final double SIGMA = 0.03;
    private static final double DRIFT = -0.0005;
    private static final double CRASH_HAZARD = 0.004;
    private static final double MAX_PRICE = 25.0;

    private final SecureRandom random;
    private double price = 1.0;

    public PricePathGenerator(SecureRandom random) {
        this.random = random;
    }

    public record Path(double[] prices, int crashTick) {
    }

    /**
     * Будує графік на maxTicks тіків. crashTick це номер тіку, на якому монета обвалюється
     * (або -1, якщо раунд дійде до кінця). Якщо продаж на якомусь тіку дав би чистий виграш більший за maxWin,
     * обвал ставиться саме туди, тож казино ніколи не заплатить більше за свій ліміт.
     */
    public Path generate(BigDecimal amount, BigDecimal maxWin, int maxTicks) {
        double stake = amount.doubleValue();
        double limit = maxWin.doubleValue();
        double[] prices = new double[maxTicks];
        prices[0] = price;
        int crashTick = -1;

        for (int i = 1; i < maxTicks; i++) {
            if (random.nextDouble() < CRASH_HAZARD) {
                crashTick = i;
                break;
            }
            double step = Math.exp(DRIFT - SIGMA * SIGMA / 2 + SIGMA * random.nextGaussian());
            price = Math.min(MAX_PRICE, Math.max(0.01, price * step));
            if (stake * price * SELL_FEE - stake > limit) {
                crashTick = i;
                break;
            }
            prices[i] = price;
        }

        if (crashTick > 0) {
            double[] cut = new double[crashTick];
            System.arraycopy(prices, 0, cut, 0, crashTick);
            prices = cut;
        }
        return new Path(prices, crashTick);
    }
}

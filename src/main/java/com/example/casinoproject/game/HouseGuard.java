package com.example.casinoproject.game;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Захист від того, щоб казино програло. Чистий виграш за одну ставку (виплата мінус ставка) не може бути більшим ніж частка від банку казино
 * (casino.house.max-win-share) і ніж абсолютний ліміт (casino.house.max-win-absolute).
 * Движки ігор отримують цю межу і, якщо випадковий результат її перевищує, перекидають його на програшний.
 */
@Component
public class HouseGuard {

    private final BigDecimal maxWinShare;
    private final BigDecimal maxWinAbsolute;

    public HouseGuard(@Value("${casino.house.max-win-share}") BigDecimal maxWinShare,
                      @Value("${casino.house.max-win-absolute}") BigDecimal maxWinAbsolute) {
        this.maxWinShare = maxWinShare;
        this.maxWinAbsolute = maxWinAbsolute;
    }

    /**
     * Повертає максимальний чистий виграш за одну ставку при поточному банку. Якщо банк порожній, повертає 0,
     * тобто будь-який виграш буде заблоковано.
     */
    public BigDecimal maxWin(BigDecimal bankBalance) {
        if (bankBalance == null || bankBalance.signum() <= 0) {
            return BigDecimal.ZERO;
        }
        BigDecimal byShare = bankBalance.multiply(maxWinShare);
        return byShare.min(maxWinAbsolute).setScale(2, RoundingMode.DOWN);
    }
}

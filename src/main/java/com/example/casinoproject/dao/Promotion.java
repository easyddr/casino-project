package com.example.casinoproject.dao;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Промокод з бонусом. validUntil може бути null, тоді акція безстрокова.
 */
public record Promotion(Long id, String code, String title, BigDecimal bonusAmount,
                        OffsetDateTime validUntil, boolean active) {

    public boolean isUsableAt(OffsetDateTime now) {
        return active && (validUntil == null || validUntil.isAfter(now));
    }
}

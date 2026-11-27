package com.example.casinoproject.game;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Результат одного раунду від движка гри: скільки виплатити і що показати гравцю (details піде в JSON ставки).
 */
public record GameOutcome(BigDecimal payout, BigDecimal multiplier, Map<String, Object> details) {
}

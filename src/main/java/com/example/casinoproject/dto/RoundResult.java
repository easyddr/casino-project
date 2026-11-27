package com.example.casinoproject.dto;

import com.example.casinoproject.domain.BetStatus;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Відповідь після раунду: що випало, скільки виплачено і який тепер баланс.
 */
public record RoundResult(Long betId, BigDecimal amount, BigDecimal payout, BetStatus status,
                          BigDecimal balance, Map<String, Object> details) {
}

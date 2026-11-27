package com.example.casinoproject.dto;

import com.example.casinoproject.domain.BetStatus;

import java.math.BigDecimal;
import java.util.List;

/**
 * Стан крипто-раунду. prices містить ціни починаючи з тіку from (щоб фронт не тягнув весь графік щоразу),
 * майбутні ціни сервер ніколи не віддає. Коли finished = true, раунд розраховано і payout фінальний.
 */
public record CryptoStateResponse(Long betId, BigDecimal amount, int from, int tick, List<Double> prices,
                                  double currentPrice, BigDecimal currentValue, boolean finished,
                                  boolean crashed, BetStatus status, BigDecimal payout, BigDecimal balance) {
}

package com.example.casinoproject.dto;

import com.example.casinoproject.domain.Bet;
import com.example.casinoproject.domain.BetStatus;
import com.example.casinoproject.domain.GameType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record BetView(Long id, String gameName, GameType gameType, BigDecimal amount, BigDecimal payout,
                      BetStatus status, String result, OffsetDateTime createdAt) {

    public static BetView from(Bet b) {
        return new BetView(b.getId(), b.getGame().getName(), b.getGame().getType(), b.getAmount(),
                b.getPayout(), b.getStatus(), b.getResult(), b.getCreatedAt());
    }
}

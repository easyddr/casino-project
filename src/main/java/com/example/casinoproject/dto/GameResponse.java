package com.example.casinoproject.dto;

import com.example.casinoproject.domain.Game;
import com.example.casinoproject.domain.GameType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record GameResponse(Long id, String name, GameType type, String description,
                           BigDecimal minBet, BigDecimal maxBet, boolean active, OffsetDateTime createdAt) {

    public static GameResponse from(Game g) {
        return new GameResponse(g.getId(), g.getName(), g.getType(), g.getDescription(),
                g.getMinBet(), g.getMaxBet(), g.isActive(), g.getCreatedAt());
    }
}

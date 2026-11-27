package com.example.casinoproject.dao;

import com.example.casinoproject.domain.TransactionType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * Рядок таблиці transactions. betId заповнюється тільки для BET, WIN і REFUND,
 * id і createdAt при створенні можна передавати як null, їх заповнить база.
 */
public record TransactionRecord(Long id, Long userId, Long betId, TransactionType type,
                                BigDecimal amount, BigDecimal balanceAfter, OffsetDateTime createdAt) {

    public static TransactionRecord of(Long userId, Long betId, TransactionType type,
                                       BigDecimal amount, BigDecimal balanceAfter) {
        return new TransactionRecord(null, userId, betId, type, amount, balanceAfter, null);
    }
}

package com.example.casinoproject.dto;

import com.example.casinoproject.repository.BetRepository;

import java.math.BigDecimal;
import java.util.List;

/**
 * Зведення для адмінки: гроші казино, гроші гравців, обороти платежів і статистика по кожній грі.
 */
public record AdminStats(BigDecimal houseBank, BigDecimal playersBalance, BigDecimal totalDeposits,
                         BigDecimal totalWithdrawals, BigDecimal totalBonuses,
                         List<BetRepository.GameStats> games) {
}

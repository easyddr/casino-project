package com.example.casinoproject.dto;

import java.math.BigDecimal;

public record WinView(String username, String gameName, BigDecimal amount, BigDecimal payout) {
}

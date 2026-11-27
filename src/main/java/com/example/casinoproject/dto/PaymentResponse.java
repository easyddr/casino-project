package com.example.casinoproject.dto;

import com.example.casinoproject.domain.Payment;
import com.example.casinoproject.domain.PaymentStatus;
import com.example.casinoproject.domain.PaymentType;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

public record PaymentResponse(Long id, PaymentType type, BigDecimal amount, PaymentStatus status,
                              String cardMask, String providerRef, String errorMessage, OffsetDateTime createdAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(p.getId(), p.getType(), p.getAmount(), p.getStatus(), p.getCardMask(),
                p.getProviderRef(), p.getErrorMessage(), p.getCreatedAt());
    }
}

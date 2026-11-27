package com.example.casinoproject.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PromotionRequest {

    @NotBlank
    @Pattern(regexp = "[A-Za-z0-9_-]{3,30}", message = "Код: 3-30 латинських літер, цифр, _ або -")
    private String code;

    @NotBlank
    @Size(max = 100)
    private String title;

    @NotNull
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    private BigDecimal bonusAmount;

    private OffsetDateTime validUntil;

    private boolean active = true;
}

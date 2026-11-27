package com.example.casinoproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record DiceRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal amount,
        @Schema(description = "Шанс виграшу у відсотках", example = "50")
        @NotNull @Min(2) @Max(95) Integer chance) {
}

package com.example.casinoproject.dto;

import com.example.casinoproject.domain.GameType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

@Schema(description = "Дані гри для створення або повного оновлення")
public record GameRequest(
        @Schema(description = "Унікальна назва", example = "Mega Slots")
        @NotBlank @Size(max = 100) String name,
        @Schema(description = "Тип гри, від нього залежить движок", example = "SLOTS")
        @NotNull GameType type,
        @Schema(description = "Опис для гравців")
        @Size(max = 2000) String description,
        @Schema(description = "Мінімальна ставка", example = "1.00")
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal minBet,
        @Schema(description = "Максимальна ставка", example = "500.00")
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal maxBet,
        @Schema(description = "Чи доступна гра гравцям", example = "true")
        @NotNull Boolean active) {
}

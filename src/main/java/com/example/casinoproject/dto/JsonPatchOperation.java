package com.example.casinoproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Одна операція JSON Patch (RFC 6902). Підтримуються add, replace, remove, test для полів верхнього рівня")
public record JsonPatchOperation(
        @Schema(example = "replace") @NotBlank String op,
        @Schema(example = "/maxBet") @NotBlank String path,
        @Schema(example = "750.00") Object value) {
}

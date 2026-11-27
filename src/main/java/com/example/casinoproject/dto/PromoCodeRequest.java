package com.example.casinoproject.dto;

import jakarta.validation.constraints.NotBlank;

public record PromoCodeRequest(@NotBlank String code) {
}

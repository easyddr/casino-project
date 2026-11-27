package com.example.casinoproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Виведення коштів на картку. Як і з поповненням, можна дати номер картки або savedPaymentId.
 */
@Data
@Schema(description = "Запит на виведення коштів")
public class WithdrawRequest {

    @NotNull
    @DecimalMin("1.00")
    @Digits(integer = 12, fraction = 2)
    @Schema(example = "50.00")
    private BigDecimal amount;

    @Schema(example = "4242 4242 4242 4242")
    private String cardNumber;

    private Long savedPaymentId;
}

package com.example.casinoproject.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

/**
 * Поповнення через псевдо-шлюз. Або вкажіть дані картки, або savedPaymentId попереднього успішного поповнення.
 * Тестові картки: 4242 4242 4242 4242 завжди проходить, 4000 0000 0000 0002 відхиляється,
 * 4000 0000 0000 0119 імітує збій шлюзу.
 */
@Data
@Schema(description = "Запит на поповнення балансу")
public class DepositRequest {

    @NotNull
    @DecimalMin("1.00")
    @Digits(integer = 12, fraction = 2)
    @Schema(example = "100.00")
    private BigDecimal amount;

    @Schema(description = "Номер картки, пробіли дозволені", example = "4242 4242 4242 4242")
    private String cardNumber;

    @Size(max = 100)
    @Schema(example = "IVAN PETRENKO")
    private String cardHolder;

    @Min(1)
    @Max(12)
    @Schema(example = "12")
    private Integer expMonth;

    @Min(2000)
    @Max(2100)
    @Schema(example = "2030")
    private Integer expYear;

    @Pattern(regexp = "^$|\\d{3,4}", message = "CVV це 3 або 4 цифри")
    @Schema(example = "123")
    private String cvv;

    @Schema(description = "id попереднього успішного поповнення, щоб використати ту саму картку")
    private Long savedPaymentId;
}

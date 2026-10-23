package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;

import com.bank.account.interfaces.rest.validation.NonZero;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

public record UpdateMovementRequest(
        @Schema(description = "New amount: positive for a deposit, negative for a withdrawal", example = "-500.00")
        @NotNull
        @NonZero
        @Digits(integer = ValidationMessages.MONEY_INTEGER_DIGITS, fraction = ValidationMessages.MONEY_FRACTION_DIGITS,
                message = ValidationMessages.MONEY)
        BigDecimal amount) {
}

package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;

import com.bank.account.interfaces.rest.validation.NonZero;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateMovementRequest(
        @Schema(example = "478758")
        @NotNull @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN, message = ValidationMessages.ACCOUNT_NUMBER)
        String accountNumber,

        @Schema(description = "Positive for a deposit, negative for a withdrawal", example = "-575.00")
        @NotNull
        @NonZero
        @Digits(integer = ValidationMessages.MONEY_INTEGER_DIGITS, fraction = ValidationMessages.MONEY_FRACTION_DIGITS,
                message = ValidationMessages.MONEY)
        BigDecimal amount) {
}

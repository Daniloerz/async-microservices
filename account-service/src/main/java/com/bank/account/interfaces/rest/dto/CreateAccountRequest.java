package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.bank.account.domain.model.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record CreateAccountRequest(
        @Schema(example = "478758")
        @NotNull @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN, message = ValidationMessages.ACCOUNT_NUMBER)
        String accountNumber,

        @Schema(example = "SAVINGS")
        @NotNull AccountType accountType,

        @Schema(example = "2000.00")
        @NotNull
        @DecimalMin(value = "0.00", message = ValidationMessages.NOT_NEGATIVE)
        @Digits(integer = ValidationMessages.MONEY_INTEGER_DIGITS, fraction = ValidationMessages.MONEY_FRACTION_DIGITS,
                message = ValidationMessages.MONEY)
        BigDecimal initialBalance,

        @Schema(example = "3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10")
        @NotNull UUID customerId,

        @Schema(description = "Defaults to true", example = "true")
        Boolean active) {
}

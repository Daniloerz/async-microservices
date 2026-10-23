package com.bank.account.interfaces.rest.dto;

import com.bank.account.domain.model.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record UpdateAccountRequest(
        @Schema(example = "CHECKING")
        @NotNull AccountType accountType,

        @Schema(example = "true")
        @NotNull Boolean active) {
}

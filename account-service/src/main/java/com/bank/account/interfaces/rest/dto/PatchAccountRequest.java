package com.bank.account.interfaces.rest.dto;

import com.bank.account.domain.model.AccountType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;

public record PatchAccountRequest(AccountType accountType, Boolean active) {

    @Schema(hidden = true)
    @AssertTrue(message = "at least one field must be provided")
    public boolean isAnyFieldPresent() {
        return accountType != null || active != null;
    }
}

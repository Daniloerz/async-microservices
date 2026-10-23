package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bank.account.domain.model.AccountType;

public record AccountResponse(
        String accountNumber,
        AccountType accountType,
        BigDecimal initialBalance,
        BigDecimal balance,
        boolean active,
        UUID customerId,
        Instant createdAt,
        Instant updatedAt) {
}

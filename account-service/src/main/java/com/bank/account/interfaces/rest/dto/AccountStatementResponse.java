package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.MovementType;

public record AccountStatementResponse(
        CustomerSummary customer,
        LocalDate startDate,
        LocalDate endDate,
        String timeZone,
        List<StatementAccount> accounts) {

    public record CustomerSummary(UUID customerId, String name, boolean active) {
    }

    public record StatementAccount(
            String accountNumber,
            AccountType accountType,
            boolean active,
            BigDecimal initialBalance,
            BigDecimal openingBalance,
            BigDecimal closingBalance,
            BigDecimal availableBalance,
            BigDecimal totalDeposits,
            BigDecimal totalWithdrawals,
            List<StatementMovement> movements) {
    }

    public record StatementMovement(
            UUID movementId,
            Instant movementDate,
            MovementType movementType,
            BigDecimal amount,
            BigDecimal balance) {
    }
}

package com.bank.account.interfaces.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.bank.account.domain.model.MovementType;

public record MovementResponse(
        UUID movementId,
        String accountNumber,
        MovementType movementType,
        BigDecimal amount,
        BigDecimal balance,
        Instant movementDate) {
}

package com.bank.account.domain.model;

import java.math.BigDecimal;

public enum MovementType {
    DEPOSIT,
    WITHDRAWAL;

    public static MovementType of(BigDecimal amount) {
        return amount.signum() > 0 ? DEPOSIT : WITHDRAWAL;
    }
}

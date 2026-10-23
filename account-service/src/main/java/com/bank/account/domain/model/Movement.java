package com.bank.account.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Movement {

    private final Long id;
    private final UUID movementId;
    private final String accountNumber;
    private MovementType movementType;
    private BigDecimal amount;
    private BigDecimal balance;
    private final Instant movementDate;

    @Builder
    private Movement(Long id, UUID movementId, String accountNumber, MovementType movementType, BigDecimal amount,
            BigDecimal balance, Instant movementDate) {
        this.id = id;
        this.movementId = Objects.requireNonNull(movementId, "movementId");
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.movementType = Objects.requireNonNull(movementType, "movementType");
        this.amount = Money.of(Objects.requireNonNull(amount, "amount"));
        this.balance = Money.of(Objects.requireNonNull(balance, "balance"));
        this.movementDate = movementDate;
    }

    static Movement record(String accountNumber, BigDecimal amount, BigDecimal balanceAfter) {
        return Movement.builder()
                .movementId(UUID.randomUUID())
                .accountNumber(accountNumber)
                .movementType(MovementType.of(amount))
                .amount(amount)
                .balance(balanceAfter)
                .build();
    }

    BigDecimal balanceBefore() {
        return balance.subtract(amount);
    }

    void correct(BigDecimal newAmount, BigDecimal newBalance) {
        this.movementType = MovementType.of(newAmount);
        this.amount = Money.of(newAmount);
        this.balance = Money.of(newBalance);
    }
}

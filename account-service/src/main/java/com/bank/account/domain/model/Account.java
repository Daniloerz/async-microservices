package com.bank.account.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bank.account.domain.exception.AccountActivationNotAllowedException;
import com.bank.account.domain.exception.InactiveAccountException;
import com.bank.account.domain.exception.InsufficientBalanceException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Account {

    private final Long id;
    private final String accountNumber;
    private AccountType accountType;
    private final BigDecimal initialBalance;
    private BigDecimal balance;
    private boolean active;
    private final UUID customerId;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private Account(Long id, String accountNumber, AccountType accountType, BigDecimal initialBalance,
            BigDecimal balance, boolean active, UUID customerId, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.accountNumber = Objects.requireNonNull(accountNumber, "accountNumber");
        this.accountType = Objects.requireNonNull(accountType, "accountType");
        this.initialBalance = Money.of(Objects.requireNonNull(initialBalance, "initialBalance"));
        this.balance = Money.of(Objects.requireNonNull(balance, "balance"));
        this.active = active;
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Account open(String accountNumber, AccountType accountType, BigDecimal initialBalance,
            UUID customerId, boolean active) {
        return Account.builder()
                .accountNumber(accountNumber)
                .accountType(accountType)
                .initialBalance(initialBalance)
                .balance(initialBalance)
                .customerId(customerId)
                .active(active)
                .build();
    }

    public void apply(AccountChanges changes, CustomerSnapshot owner) {
        if (Boolean.TRUE.equals(changes.active()) && !active && !owner.active()) {
            throw new AccountActivationNotAllowedException();
        }
        this.accountType = Objects.requireNonNullElse(changes.accountType(), accountType);
        this.active = Objects.requireNonNullElse(changes.active(), active);
    }

    public void deactivate() {
        this.active = false;
    }

    public Movement registerMovement(BigDecimal amount) {
        ensureActive();
        BigDecimal newBalance = balanceAfter(balance, amount);
        this.balance = newBalance;
        return Movement.record(accountNumber, amount, newBalance);
    }

    public void correctMovement(Movement lastMovement, BigDecimal newAmount) {
        if (!accountNumber.equals(lastMovement.getAccountNumber())) {
            throw new IllegalArgumentException("Movement does not belong to this account");
        }
        ensureActive();
        BigDecimal newBalance = balanceAfter(lastMovement.balanceBefore(), newAmount);
        lastMovement.correct(newAmount, newBalance);
        this.balance = newBalance;
    }

    private void ensureActive() {
        if (!active) {
            throw new InactiveAccountException();
        }
    }

    private static BigDecimal balanceAfter(BigDecimal startingBalance, BigDecimal amount) {
        if (amount.signum() == 0) {
            throw new IllegalArgumentException("Movement amount must not be zero");
        }
        BigDecimal result = Money.of(startingBalance.add(amount));
        if (result.signum() < 0) {
            throw new InsufficientBalanceException();
        }
        return result;
    }
}

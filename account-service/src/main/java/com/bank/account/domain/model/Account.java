package com.bank.account.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import com.bank.account.domain.exception.AccountActivationNotAllowedException;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class Account {

    public static final int MONEY_SCALE = 2;

    private final Long id;
    private final String accountNumber;
    private AccountType accountType;
    private final BigDecimal initialBalance;
    private final BigDecimal balance;
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
        this.initialBalance = toMoney(Objects.requireNonNull(initialBalance, "initialBalance"));
        this.balance = toMoney(Objects.requireNonNull(balance, "balance"));
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

    private static BigDecimal toMoney(BigDecimal amount) {
        return amount.setScale(MONEY_SCALE, RoundingMode.UNNECESSARY);
    }
}

package com.bank.account.application.report;

import java.math.BigDecimal;
import java.util.List;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.Movement;

public record AccountStatementAccount(
        String accountNumber,
        AccountType accountType,
        boolean active,
        BigDecimal initialBalance,
        BigDecimal openingBalance,
        BigDecimal closingBalance,
        BigDecimal availableBalance,
        BigDecimal totalDeposits,
        BigDecimal totalWithdrawals,
        List<Movement> movements) {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    public AccountStatementAccount {
        movements = movements == null ? List.of() : List.copyOf(movements);
    }

    public static AccountStatementAccount from(Account account, BigDecimal openingBalance, List<Movement> periodMovements) {
        BigDecimal closingBalance = periodMovements.isEmpty()
                ? openingBalance
                : periodMovements.getLast().getBalance();
        BigDecimal deposits = periodMovements.stream()
                .map(Movement::getAmount)
                .filter(amount -> amount.signum() > 0)
                .reduce(ZERO, BigDecimal::add);
        BigDecimal withdrawals = periodMovements.stream()
                .map(Movement::getAmount)
                .filter(amount -> amount.signum() < 0)
                .map(BigDecimal::negate)
                .reduce(ZERO, BigDecimal::add);

        return new AccountStatementAccount(account.getAccountNumber(), account.getAccountType(), account.isActive(),
                account.getInitialBalance(), openingBalance, closingBalance, account.getBalance(), deposits,
                withdrawals, periodMovements);
    }
}

package com.bank.account.application.report;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.Movement;

class AccountStatementAccountTest {

    @Test
    void balancesAndTotalsAreCalculatedFromThePeriodMovements() {
        Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000"), UUID.randomUUID(), true);
        Movement before = account.registerMovement(new BigDecimal("-500"));
        Movement first = account.registerMovement(new BigDecimal("-575"));
        Movement second = account.registerMovement(new BigDecimal("100"));

        AccountStatementAccount line = AccountStatementAccount.from(account, before.getBalance(), List.of(first, second));

        assertThat(line.initialBalance()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(line.openingBalance()).isEqualTo(new BigDecimal("1500.00"));
        assertThat(line.closingBalance()).isEqualTo(new BigDecimal("1025.00"));
        assertThat(line.availableBalance()).isEqualTo(new BigDecimal("1025.00"));
        assertThat(line.totalDeposits()).isEqualTo(new BigDecimal("100.00"));
        assertThat(line.totalWithdrawals()).isEqualTo(new BigDecimal("575.00"));
        assertThat(line.openingBalance().add(line.totalDeposits()).subtract(line.totalWithdrawals()))
                .isEqualTo(line.closingBalance());
        assertThat(line.movements()).containsExactly(first, second);
    }

    @Test
    void accountWithoutMovementsKeepsItsOpeningBalance() {
        Account account = Account.open("495878", AccountType.SAVINGS, BigDecimal.ZERO, UUID.randomUUID(), false);

        AccountStatementAccount line = AccountStatementAccount.from(account, account.getInitialBalance(), List.of());

        assertThat(line.openingBalance()).isEqualTo(new BigDecimal("0.00"));
        assertThat(line.closingBalance()).isEqualTo(new BigDecimal("0.00"));
        assertThat(line.totalDeposits()).isEqualTo(new BigDecimal("0.00"));
        assertThat(line.totalWithdrawals()).isEqualTo(new BigDecimal("0.00"));
        assertThat(line.active()).isFalse();
        assertThat(line.movements()).isEmpty();
    }
}

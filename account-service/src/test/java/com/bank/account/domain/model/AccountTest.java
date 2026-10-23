package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bank.account.domain.exception.AccountActivationNotAllowedException;

class AccountTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("2000.00");

    @Test
    void openStartsWithTheInitialBalanceAsAvailableBalance() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        assertThat(account.getId()).isNull();
        assertThat(account.getAccountNumber()).isEqualTo("478758");
        assertThat(account.getAccountType()).isEqualTo(AccountType.SAVINGS);
        assertThat(account.getInitialBalance()).isEqualByComparingTo("2000");
        assertThat(account.getBalance()).isEqualByComparingTo("2000");
        assertThat(account.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(account.isActive()).isTrue();
    }

    @Test
    void amountsAlwaysHaveTwoDecimals() {
        Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000"), CUSTOMER_ID, true);

        assertThat(account.getInitialBalance()).isEqualTo(new BigDecimal("2000.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("2000.00"));
    }

    @Test
    void amountsWithMoreThanTwoDecimalsAreRejected() {
        assertThatThrownBy(() -> Account.open("478758", AccountType.SAVINGS, new BigDecimal("1.005"), CUSTOMER_ID, true))
                .isInstanceOf(ArithmeticException.class);
    }

    @Test
    void applyChangesOnlyTheProvidedFields() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        account.apply(new AccountChanges(AccountType.CHECKING, null), activeOwner());

        assertThat(account.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(account.isActive()).isTrue();
        assertThat(account.getInitialBalance()).isEqualByComparingTo("2000");
    }

    @Test
    void accountCanBeDeactivatedEvenIfTheOwnerIsInactive() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        account.apply(new AccountChanges(null, false), inactiveOwner());

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void inactiveAccountCannotBeActivatedWhileTheOwnerIsInactive() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, false);

        assertThatThrownBy(() -> account.apply(new AccountChanges(null, true), inactiveOwner()))
                .isInstanceOf(AccountActivationNotAllowedException.class);
        assertThat(account.isActive()).isFalse();
    }

    @Test
    void inactiveAccountCanBeActivatedWhenTheOwnerIsActive() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, false);

        account.apply(new AccountChanges(null, true), activeOwner());

        assertThat(account.isActive()).isTrue();
    }

    @Test
    void keepingAnActiveAccountActiveIsAllowedForAnInactiveOwner() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        account.apply(new AccountChanges(AccountType.CHECKING, true), inactiveOwner());

        assertThat(account.isActive()).isTrue();
        assertThat(account.getAccountType()).isEqualTo(AccountType.CHECKING);
    }

    @Test
    void deactivateMakesTheAccountInactive() {
        Account account = Account.open("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        account.deactivate();

        assertThat(account.isActive()).isFalse();
    }

    @Test
    void emptyChangesAreDetected() {
        assertThat(new AccountChanges(null, null).isEmpty()).isTrue();
        assertThat(new AccountChanges(null, false).isEmpty()).isFalse();
    }

    private static CustomerSnapshot activeOwner() {
        return new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", true, Instant.parse("2026-10-22T10:00:00Z"));
    }

    private static CustomerSnapshot inactiveOwner() {
        return new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", false, Instant.parse("2026-10-22T10:00:00Z"));
    }
}

package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bank.account.domain.exception.InactiveAccountException;
import com.bank.account.domain.exception.InsufficientBalanceException;

class AccountMovementTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void depositIncreasesTheBalance() {
        Account account = account("100", true);

        Movement movement = account.registerMovement(new BigDecimal("600"));

        assertThat(movement.getMovementType()).isEqualTo(MovementType.DEPOSIT);
        assertThat(movement.getAmount()).isEqualTo(new BigDecimal("600.00"));
        assertThat(movement.getBalance()).isEqualTo(new BigDecimal("700.00"));
        assertThat(movement.getAccountNumber()).isEqualTo("225487");
        assertThat(movement.getMovementId()).isNotNull();
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("700.00"));
    }

    @Test
    void withdrawalDecreasesTheBalance() {
        Account account = account("2000", true);

        Movement movement = account.registerMovement(new BigDecimal("-575"));

        assertThat(movement.getMovementType()).isEqualTo(MovementType.WITHDRAWAL);
        assertThat(movement.getBalance()).isEqualTo(new BigDecimal("1425.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("1425.00"));
    }

    @Test
    void withdrawalCanLeaveTheBalanceExactlyAtZero() {
        Account account = account("540", true);

        Movement movement = account.registerMovement(new BigDecimal("-540"));

        assertThat(movement.getBalance()).isEqualByComparingTo("0");
        assertThat(account.getBalance()).isEqualByComparingTo("0");
    }

    @Test
    void withdrawalWithoutEnoughBalanceIsRejectedAndChangesNothing() {
        Account account = account("100", true);

        assertThatThrownBy(() -> account.registerMovement(new BigDecimal("-100.01")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessage("Insufficient balance");
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("100.00"));
    }

    @Test
    void inactiveAccountRejectsMovements() {
        Account account = account("100", false);

        assertThatThrownBy(() -> account.registerMovement(new BigDecimal("10")))
                .isInstanceOf(InactiveAccountException.class)
                .hasMessage("Account is not active");
    }

    @Test
    void zeroAmountIsRejected() {
        Account account = account("100", true);

        assertThatIllegalArgumentException().isThrownBy(() -> account.registerMovement(BigDecimal.ZERO));
    }

    @Test
    void correctingTheLastMovementRecalculatesTypeAndBalances() {
        Account account = account("100", true);
        Movement movement = account.registerMovement(new BigDecimal("600"));

        account.correctMovement(movement, new BigDecimal("-40"));

        assertThat(movement.getMovementType()).isEqualTo(MovementType.WITHDRAWAL);
        assertThat(movement.getAmount()).isEqualTo(new BigDecimal("-40.00"));
        assertThat(movement.getBalance()).isEqualTo(new BigDecimal("60.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("60.00"));
    }

    @Test
    void correctionThatWouldOverdrawIsRejectedAndChangesNothing() {
        Account account = account("100", true);
        Movement movement = account.registerMovement(new BigDecimal("-50"));

        assertThatThrownBy(() -> account.correctMovement(movement, new BigDecimal("-150")))
                .isInstanceOf(InsufficientBalanceException.class);
        assertThat(movement.getAmount()).isEqualTo(new BigDecimal("-50.00"));
        assertThat(movement.getBalance()).isEqualTo(new BigDecimal("50.00"));
        assertThat(account.getBalance()).isEqualTo(new BigDecimal("50.00"));
    }

    @Test
    void correctionIsRejectedOnAnInactiveAccount() {
        Account account = account("100", true);
        Movement movement = account.registerMovement(new BigDecimal("10"));
        account.deactivate();

        assertThatThrownBy(() -> account.correctMovement(movement, new BigDecimal("20")))
                .isInstanceOf(InactiveAccountException.class);
    }

    @Test
    void movementOfAnotherAccountCannotBeCorrected() {
        Account account = account("100", true);
        Movement foreign = account("100", true, "999999").registerMovement(BigDecimal.TEN);

        assertThatIllegalArgumentException().isThrownBy(() -> account.correctMovement(foreign, BigDecimal.ONE));
    }

    private static Account account(String initialBalance, boolean active) {
        return account(initialBalance, active, "225487");
    }

    private static Account account(String initialBalance, boolean active, String accountNumber) {
        return Account.open(accountNumber, AccountType.CHECKING, new BigDecimal(initialBalance), CUSTOMER_ID, active);
    }
}

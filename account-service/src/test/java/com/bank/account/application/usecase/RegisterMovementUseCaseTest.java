package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.application.exception.AccountNotAvailableException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.domain.exception.InsufficientBalanceException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.Movement;
import com.bank.account.domain.model.MovementType;

@ExtendWith(MockitoExtension.class)
class RegisterMovementUseCaseTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private MovementRepository movementRepository;

    private RegisterMovementUseCase registerMovementUseCase;

    @BeforeEach
    void setUp() {
        registerMovementUseCase = new RegisterMovementUseCase(accountRepository, movementRepository);
    }

    @Test
    void movementIsRegisteredOnTheLockedAccountAndBothAreSaved() {
        Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000"), UUID.randomUUID(), true);
        when(accountRepository.findForUpdate("478758")).thenReturn(Optional.of(account));
        when(movementRepository.save(any(Movement.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Movement movement = registerMovementUseCase.register("478758", new BigDecimal("-575"));

        assertThat(movement.getMovementType()).isEqualTo(MovementType.WITHDRAWAL);
        assertThat(movement.getBalance()).isEqualByComparingTo("1425");
        InOrder order = inOrder(accountRepository, movementRepository);
        order.verify(accountRepository).findForUpdate("478758");
        order.verify(accountRepository).save(account);
        order.verify(movementRepository).save(movement);
    }

    @Test
    void unknownAccountIsRejected() {
        when(accountRepository.findForUpdate("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> registerMovementUseCase.register("999999", BigDecimal.TEN))
                .isInstanceOf(AccountNotAvailableException.class)
                .hasMessage("Account does not exist");

        verifyNoInteractions(movementRepository);
    }

    @Test
    void insufficientBalanceSavesNothing() {
        Account account = Account.open("496825", AccountType.SAVINGS, new BigDecimal("540"), UUID.randomUUID(), true);
        when(accountRepository.findForUpdate("496825")).thenReturn(Optional.of(account));

        assertThatThrownBy(() -> registerMovementUseCase.register("496825", new BigDecimal("-541")))
                .isInstanceOf(InsufficientBalanceException.class);

        verify(accountRepository, never()).save(any());
        verifyNoInteractions(movementRepository);
    }
}

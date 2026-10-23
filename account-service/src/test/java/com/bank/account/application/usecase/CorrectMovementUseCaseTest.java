package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.application.exception.MovementNotCorrectableException;
import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.Movement;
import com.bank.account.domain.model.MovementType;

@ExtendWith(MockitoExtension.class)
class CorrectMovementUseCaseTest {

    @Mock
    private MovementRepository movementRepository;

    @Mock
    private AccountRepository accountRepository;

    private CorrectMovementUseCase correctMovementUseCase;
    private Account account;

    @BeforeEach
    void setUp() {
        correctMovementUseCase = new CorrectMovementUseCase(movementRepository, accountRepository);
        account = Account.open("225487", AccountType.CHECKING, new BigDecimal("100"), UUID.randomUUID(), true);
    }

    @Test
    void lastMovementIsCorrectedAndTheAccountBalanceFollows() {
        Movement last = account.registerMovement(new BigDecimal("600"));
        when(movementRepository.findByMovementId(last.getMovementId())).thenReturn(Optional.of(last));
        when(accountRepository.findForUpdate("225487")).thenReturn(Optional.of(account));
        when(movementRepository.findLastOfAccount("225487")).thenReturn(Optional.of(last));
        when(movementRepository.save(last)).thenReturn(last);

        Movement corrected = correctMovementUseCase.correct(last.getMovementId(), new BigDecimal("500"));

        assertThat(corrected.getMovementType()).isEqualTo(MovementType.DEPOSIT);
        assertThat(corrected.getBalance()).isEqualByComparingTo("600");
        assertThat(account.getBalance()).isEqualByComparingTo("600");
        verify(accountRepository).save(account);
    }

    @Test
    void olderMovementCannotBeCorrected() {
        Movement older = account.registerMovement(new BigDecimal("600"));
        Movement last = account.registerMovement(new BigDecimal("-50"));
        when(movementRepository.findByMovementId(older.getMovementId())).thenReturn(Optional.of(older));
        when(accountRepository.findForUpdate("225487")).thenReturn(Optional.of(account));
        when(movementRepository.findLastOfAccount("225487")).thenReturn(Optional.of(last));

        assertThatThrownBy(() -> correctMovementUseCase.correct(older.getMovementId(), BigDecimal.ONE))
                .isInstanceOf(MovementNotCorrectableException.class)
                .hasMessage("Only the last movement of an account can be corrected");

        verify(accountRepository, never()).save(any());
        verify(movementRepository, never()).save(any());
    }

    @Test
    void unknownMovementIsReportedAsNotFound() {
        UUID unknownMovementId = UUID.randomUUID();
        when(movementRepository.findByMovementId(unknownMovementId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> correctMovementUseCase.correct(unknownMovementId, BigDecimal.ONE))
                .isInstanceOf(MovementNotFoundException.class)
                .hasMessage("Movement not found");
    }
}

package com.bank.account.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.domain.exception.AccountActivationNotAllowedException;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountChanges;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;

@ExtendWith(MockitoExtension.class)
class UpdateAccountUseCaseTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    private UpdateAccountUseCase updateAccountUseCase;

    @BeforeEach
    void setUp() {
        updateAccountUseCase = new UpdateAccountUseCase(accountRepository, customerSnapshotRepository);
    }

    @Test
    void changesAreAppliedAndSaved() {
        Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000.00"), CUSTOMER_ID, true);
        when(accountRepository.findForUpdate("478758")).thenReturn(Optional.of(account));
        when(customerSnapshotRepository.findWithSharedLock(CUSTOMER_ID)).thenReturn(Optional.of(owner(true)));
        when(accountRepository.save(account)).thenReturn(account);

        Account updated = updateAccountUseCase.update("478758", new AccountChanges(AccountType.CHECKING, false));

        assertThat(updated.getAccountType()).isEqualTo(AccountType.CHECKING);
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void activationIsRejectedWhileTheOwnerIsInactive() {
        Account account = Account.open("478758", AccountType.SAVINGS, new BigDecimal("2000.00"), CUSTOMER_ID, false);
        when(accountRepository.findForUpdate("478758")).thenReturn(Optional.of(account));
        when(customerSnapshotRepository.findWithSharedLock(CUSTOMER_ID)).thenReturn(Optional.of(owner(false)));

        assertThatThrownBy(() -> updateAccountUseCase.update("478758", new AccountChanges(null, true)))
                .isInstanceOf(AccountActivationNotAllowedException.class);

        verify(accountRepository, never()).save(any());
    }

    @Test
    void unknownAccountIsReportedAsNotFound() {
        when(accountRepository.findForUpdate("999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateAccountUseCase.update("999999", new AccountChanges(null, false)))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessage("Account not found");
    }

    private static CustomerSnapshot owner(boolean active) {
        return new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", active, Instant.parse("2026-10-22T10:00:00Z"));
    }
}

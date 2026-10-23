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

import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;

@ExtendWith(MockitoExtension.class)
class CreateAccountUseCaseTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final BigDecimal INITIAL_BALANCE = new BigDecimal("2000.00");

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    private CreateAccountUseCase createAccountUseCase;

    @BeforeEach
    void setUp() {
        createAccountUseCase = new CreateAccountUseCase(accountRepository, customerSnapshotRepository);
    }

    @Test
    void accountIsOpenedForAnActiveCustomer() {
        when(customerSnapshotRepository.findForAccountOpening(CUSTOMER_ID)).thenReturn(Optional.of(owner(true)));
        when(accountRepository.existsByAccountNumber("478758")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account created = createAccountUseCase.create("478758", AccountType.SAVINGS, INITIAL_BALANCE, CUSTOMER_ID, true);

        assertThat(created.getBalance()).isEqualByComparingTo("2000");
        assertThat(created.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(created.isActive()).isTrue();
    }

    @Test
    void unknownCustomerIsRejected() {
        when(customerSnapshotRepository.findForAccountOpening(CUSTOMER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> createAccountUseCase.create("478758", AccountType.SAVINGS, INITIAL_BALANCE,
                CUSTOMER_ID, true))
                .isInstanceOf(CustomerNotAvailableException.class)
                .hasMessage("Customer does not exist or is not yet known by this service");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void inactiveCustomerIsRejected() {
        when(customerSnapshotRepository.findForAccountOpening(CUSTOMER_ID)).thenReturn(Optional.of(owner(false)));

        assertThatThrownBy(() -> createAccountUseCase.create("478758", AccountType.SAVINGS, INITIAL_BALANCE,
                CUSTOMER_ID, true))
                .isInstanceOf(CustomerNotAvailableException.class)
                .hasMessage("Customer is not active");

        verify(accountRepository, never()).save(any());
    }

    @Test
    void duplicatedAccountNumberIsRejected() {
        when(customerSnapshotRepository.findForAccountOpening(CUSTOMER_ID)).thenReturn(Optional.of(owner(true)));
        when(accountRepository.existsByAccountNumber("478758")).thenReturn(true);

        assertThatThrownBy(() -> createAccountUseCase.create("478758", AccountType.SAVINGS, INITIAL_BALANCE,
                CUSTOMER_ID, true))
                .isInstanceOf(DuplicateAccountNumberException.class);

        verify(accountRepository, never()).save(any());
    }

    private static CustomerSnapshot owner(boolean active) {
        return new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", active, Instant.parse("2026-10-22T10:00:00Z"));
    }
}

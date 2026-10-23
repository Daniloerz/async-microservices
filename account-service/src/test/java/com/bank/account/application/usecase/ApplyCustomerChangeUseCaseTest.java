package com.bank.account.application.usecase;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.account.application.command.CustomerChangeCommand;
import com.bank.account.application.command.CustomerChangeType;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.application.port.out.ProcessedEventRepository;
import com.bank.account.domain.model.CustomerSnapshot;

@ExtendWith(MockitoExtension.class)
class ApplyCustomerChangeUseCaseTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-10-22T10:00:00Z");

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private CustomerSnapshotRepository customerSnapshotRepository;

    @Mock
    private AccountRepository accountRepository;

    private ApplyCustomerChangeUseCase applyCustomerChangeUseCase;

    @BeforeEach
    void setUp() {
        applyCustomerChangeUseCase = new ApplyCustomerChangeUseCase(processedEventRepository,
                customerSnapshotRepository, accountRepository);
    }

    @Test
    void createdEventStoresTheCustomerInTheProjection() {
        CustomerChangeCommand command = command(CustomerChangeType.CREATED, NOW, true);
        when(processedEventRepository.markAsProcessed(command.eventId())).thenReturn(true);
        when(customerSnapshotRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        applyCustomerChangeUseCase.apply(command);

        verify(customerSnapshotRepository).save(new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", true, NOW));
        verifyNoInteractions(accountRepository);
    }

    @Test
    void alreadyProcessedEventIsIgnored() {
        CustomerChangeCommand command = command(CustomerChangeType.DELETED, NOW, false);
        when(processedEventRepository.markAsProcessed(command.eventId())).thenReturn(false);

        applyCustomerChangeUseCase.apply(command);

        verifyNoInteractions(customerSnapshotRepository, accountRepository);
    }

    @Test
    void eventOlderThanTheCurrentStateIsIgnored() {
        CustomerChangeCommand command = command(CustomerChangeType.UPDATED, NOW.minusSeconds(60), true);
        when(processedEventRepository.markAsProcessed(command.eventId())).thenReturn(true);
        when(customerSnapshotRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.of(new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", false, NOW)));

        applyCustomerChangeUseCase.apply(command);

        verify(customerSnapshotRepository, never()).save(any());
        verifyNoInteractions(accountRepository);
    }

    @Test
    void deletedEventDeactivatesTheCustomerAndAllItsAccounts() {
        CustomerChangeCommand command = command(CustomerChangeType.DELETED, NOW, true);
        when(processedEventRepository.markAsProcessed(command.eventId())).thenReturn(true);
        when(customerSnapshotRepository.findById(CUSTOMER_ID))
                .thenReturn(Optional.of(new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", true, NOW.minusSeconds(60))));

        applyCustomerChangeUseCase.apply(command);

        verify(customerSnapshotRepository).save(new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", false, NOW));
        verify(accountRepository).deactivateAllOfCustomer(CUSTOMER_ID);
    }

    @Test
    void inactiveCustomerUpdateDoesNotTouchTheAccounts() {
        CustomerChangeCommand command = command(CustomerChangeType.UPDATED, NOW, false);
        when(processedEventRepository.markAsProcessed(command.eventId())).thenReturn(true);
        when(customerSnapshotRepository.findById(CUSTOMER_ID)).thenReturn(Optional.empty());

        applyCustomerChangeUseCase.apply(command);

        verify(customerSnapshotRepository).save(new CustomerSnapshot(CUSTOMER_ID, "Jose Lema", false, NOW));
        verifyNoInteractions(accountRepository);
    }

    private static CustomerChangeCommand command(CustomerChangeType type, Instant occurredAt, boolean active) {
        return new CustomerChangeCommand(UUID.randomUUID(), type, occurredAt, CUSTOMER_ID, "Jose Lema", active);
    }
}

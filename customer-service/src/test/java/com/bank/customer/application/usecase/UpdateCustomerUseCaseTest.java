package com.bank.customer.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PasswordHasher;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerChanges;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@ExtendWith(MockitoExtension.class)
class UpdateCustomerUseCaseTest {

    private static final String PASSWORD_HASH = "$2a$10$hash";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private CustomerEventPublisher customerEventPublisher;

    private UpdateCustomerUseCase updateCustomerUseCase;
    private Customer existingCustomer;

    @BeforeEach
    void setUp() {
        updateCustomerUseCase = new UpdateCustomerUseCase(customerRepository, passwordHasher, customerEventPublisher);
        existingCustomer = Customer.create(
                new CustomerDetails("Jose Lema", Gender.MALE, 35, "1712345678", "Otavalo sn y principal", "098254785"),
                PASSWORD_HASH, true);
    }

    @Test
    void partialUpdateChangesOnlyProvidedFieldsAndPublishesUpdatedEvent() {
        UUID customerId = existingCustomer.getCustomerId();
        when(customerRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.save(existingCustomer)).thenReturn(existingCustomer);

        Customer updated = updateCustomerUseCase.update(customerId, onlyActive(false), null);

        assertThat(updated.isActive()).isFalse();
        assertThat(updated.getName()).isEqualTo("Jose Lema");
        assertThat(updated.getPasswordHash()).isEqualTo(PASSWORD_HASH);
        verifyNoInteractions(passwordHasher);
        verify(customerEventPublisher).publish(CustomerEventType.CUSTOMER_UPDATED, existingCustomer);
    }

    @Test
    void newPasswordIsHashedBeforeSaving() {
        UUID customerId = existingCustomer.getCustomerId();
        when(customerRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCustomer));
        when(passwordHasher.hash("5678")).thenReturn("$2a$10$newHash");
        when(customerRepository.save(existingCustomer)).thenReturn(existingCustomer);

        Customer updated = updateCustomerUseCase.update(customerId, onlyActive(null), "5678");

        assertThat(updated.getPasswordHash()).isEqualTo("$2a$10$newHash");
    }

    @Test
    void identificationUsedByAnotherCustomerIsRejected() {
        UUID customerId = existingCustomer.getCustomerId();
        when(customerRepository.findByCustomerId(customerId)).thenReturn(Optional.of(existingCustomer));
        when(customerRepository.existsByIdentificationForOtherCustomer("0999999999", customerId)).thenReturn(true);
        CustomerChanges changes = new CustomerChanges(null, null, null, "0999999999", null, null, null);

        assertThatThrownBy(() -> updateCustomerUseCase.update(customerId, changes, null))
                .isInstanceOf(DuplicateIdentificationException.class);

        verify(customerRepository, never()).save(any());
        verifyNoInteractions(customerEventPublisher);
    }

    @Test
    void unknownCustomerIsReportedAsNotFound() {
        UUID unknownCustomerId = UUID.randomUUID();
        when(customerRepository.findByCustomerId(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> updateCustomerUseCase.update(unknownCustomerId, onlyActive(false), null))
                .isInstanceOf(CustomerNotFoundException.class);

        verifyNoInteractions(customerEventPublisher);
    }

    private static CustomerChanges onlyActive(Boolean active) {
        return new CustomerChanges(null, null, null, null, null, null, active);
    }
}

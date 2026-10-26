package com.bank.customer.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@ExtendWith(MockitoExtension.class)
class DeleteCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private CustomerEventPublisher customerEventPublisher;

    private DeleteCustomerUseCase deleteCustomerUseCase;

    @BeforeEach
    void setUp() {
        deleteCustomerUseCase = new DeleteCustomerUseCase(customerRepository, customerEventPublisher);
    }

    @Test
    void deleteRemovesCustomerAndPublishesDeletedEvent() {
        Customer customer = Customer.create(
                new CustomerDetails("Juan Osorio", Gender.MALE, 40, "1700000003", "13 junio y Equinoccial", "098874587"),
                "$2a$10$hash", true);
        when(customerRepository.findForUpdate(customer.getCustomerId())).thenReturn(Optional.of(customer));

        deleteCustomerUseCase.delete(customer.getCustomerId());

        InOrder order = inOrder(customerRepository, customerEventPublisher);
        order.verify(customerRepository).delete(customer);
        order.verify(customerEventPublisher).publish(CustomerEventType.CUSTOMER_DELETED, customer);
    }

    @Test
    void unknownCustomerIsReportedAsNotFound() {
        UUID unknownCustomerId = UUID.randomUUID();
        when(customerRepository.findForUpdate(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> deleteCustomerUseCase.delete(unknownCustomerId))
                .isInstanceOf(CustomerNotFoundException.class);

        verify(customerRepository, never()).delete(any());
        verifyNoInteractions(customerEventPublisher);
    }
}

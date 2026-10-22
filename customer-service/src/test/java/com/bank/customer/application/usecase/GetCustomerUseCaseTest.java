package com.bank.customer.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@ExtendWith(MockitoExtension.class)
class GetCustomerUseCaseTest {

    @Mock
    private CustomerRepository customerRepository;

    private GetCustomerUseCase getCustomerUseCase;

    @BeforeEach
    void setUp() {
        getCustomerUseCase = new GetCustomerUseCase(customerRepository);
    }

    @Test
    void existingCustomerIsReturned() {
        Customer customer = Customer.create(
                new CustomerDetails("Marianela Montalvo", Gender.FEMALE, 30, "1700000002", "Amazonas y NNUU", "097548965"),
                "$2a$10$hash", true);
        when(customerRepository.findByCustomerId(customer.getCustomerId())).thenReturn(Optional.of(customer));

        assertThat(getCustomerUseCase.get(customer.getCustomerId())).isSameAs(customer);
    }

    @Test
    void unknownCustomerIsReportedAsNotFound() {
        UUID unknownCustomerId = UUID.randomUUID();
        when(customerRepository.findByCustomerId(unknownCustomerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> getCustomerUseCase.get(unknownCustomerId))
                .isInstanceOf(CustomerNotFoundException.class)
                .hasMessage("Customer not found");
    }
}

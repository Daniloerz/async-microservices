package com.bank.customer.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PasswordHasher;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerDetails;
import com.bank.customer.domain.model.Gender;

@ExtendWith(MockitoExtension.class)
class CreateCustomerUseCaseTest {

    private static final String RAW_PASSWORD = "1234";
    private static final String PASSWORD_HASH = "$2a$10$hash";

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private CustomerEventPublisher customerEventPublisher;

    private CreateCustomerUseCase createCustomerUseCase;

    @BeforeEach
    void setUp() {
        createCustomerUseCase = new CreateCustomerUseCase(customerRepository, passwordHasher, customerEventPublisher);
    }

    @Test
    void createStoresHashedPasswordAndPublishesCreatedEvent() {
        when(customerRepository.existsByIdentification("1712345678")).thenReturn(false);
        when(passwordHasher.hash(RAW_PASSWORD)).thenReturn(PASSWORD_HASH);
        when(customerRepository.save(any(Customer.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Customer created = createCustomerUseCase.create(details(), RAW_PASSWORD, true);

        ArgumentCaptor<Customer> savedCustomer = ArgumentCaptor.forClass(Customer.class);
        verify(customerRepository).save(savedCustomer.capture());
        assertThat(savedCustomer.getValue().getPasswordHash()).isEqualTo(PASSWORD_HASH);
        assertThat(savedCustomer.getValue().isActive()).isTrue();
        verify(customerEventPublisher).publish(CustomerEventType.CUSTOMER_CREATED, created);
    }

    @Test
    void createRejectsDuplicatedIdentificationWithoutSavingOrPublishing() {
        when(customerRepository.existsByIdentification("1712345678")).thenReturn(true);

        assertThatThrownBy(() -> createCustomerUseCase.create(details(), RAW_PASSWORD, true))
                .isInstanceOf(DuplicateIdentificationException.class);

        verify(customerRepository, never()).save(any());
        verifyNoInteractions(passwordHasher, customerEventPublisher);
    }

    private static CustomerDetails details() {
        return new CustomerDetails("Jose Lema", Gender.MALE, 35, "1712345678", "Otavalo sn y principal", "098254785");
    }
}

package com.bank.customer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PasswordHasher;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerDetails;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final PasswordHasher passwordHasher;
    private final CustomerEventPublisher customerEventPublisher;

    @Transactional
    public Customer create(CustomerDetails details, String rawPassword, boolean active) {
        if (customerRepository.existsByIdentification(details.identification())) {
            throw new DuplicateIdentificationException();
        }
        Customer customer = Customer.create(details, passwordHasher.hash(rawPassword), active);
        Customer saved = customerRepository.save(customer);
        customerEventPublisher.publish(CustomerEventType.CUSTOMER_CREATED, saved);
        return saved;
    }
}

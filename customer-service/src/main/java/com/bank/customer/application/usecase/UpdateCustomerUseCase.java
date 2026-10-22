package com.bank.customer.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PasswordHasher;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.domain.model.CustomerChanges;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final PasswordHasher passwordHasher;
    private final CustomerEventPublisher customerEventPublisher;

    @Transactional
    public Customer update(UUID customerId, CustomerChanges changes, String newPassword) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        if (changes.identification() != null
                && customerRepository.existsByIdentificationForOtherCustomer(changes.identification(), customerId)) {
            throw new DuplicateIdentificationException();
        }

        customer.apply(changes);
        if (newPassword != null) {
            customer.changePassword(passwordHasher.hash(newPassword));
        }

        Customer saved = customerRepository.save(customer);
        customerEventPublisher.publish(CustomerEventType.CUSTOMER_UPDATED, saved);
        return saved;
    }
}

package com.bank.customer.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DeleteCustomerUseCase {

    private final CustomerRepository customerRepository;
    private final CustomerEventPublisher customerEventPublisher;

    @Transactional
    public void delete(UUID customerId) {
        Customer customer = customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
        customerRepository.delete(customer);
        customerEventPublisher.publish(CustomerEventType.CUSTOMER_DELETED, customer);
    }
}

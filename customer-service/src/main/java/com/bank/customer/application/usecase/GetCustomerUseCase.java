package com.bank.customer.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetCustomerUseCase {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public Customer get(UUID customerId) {
        return customerRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));
    }
}

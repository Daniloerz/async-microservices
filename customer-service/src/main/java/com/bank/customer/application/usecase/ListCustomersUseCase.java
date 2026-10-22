package com.bank.customer.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PageResult;
import com.bank.customer.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListCustomersUseCase {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public PageResult<Customer> list(int page, int size) {
        return customerRepository.findAll(page, size);
    }
}

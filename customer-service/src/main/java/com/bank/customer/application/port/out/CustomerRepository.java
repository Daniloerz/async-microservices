package com.bank.customer.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.bank.customer.domain.model.Customer;

public interface CustomerRepository {

    Customer save(Customer customer);

    Optional<Customer> findByCustomerId(UUID customerId);

    PageResult<Customer> findAll(int page, int size);

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationForOtherCustomer(String identification, UUID customerId);

    void delete(Customer customer);
}

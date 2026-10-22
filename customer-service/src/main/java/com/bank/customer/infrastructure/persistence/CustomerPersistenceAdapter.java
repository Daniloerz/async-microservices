package com.bank.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;

import com.bank.customer.application.exception.ConcurrentCustomerChangeException;
import com.bank.customer.application.exception.CustomerNotFoundException;
import com.bank.customer.application.exception.DuplicateIdentificationException;
import com.bank.customer.application.port.out.CustomerRepository;
import com.bank.customer.application.port.out.PageResult;
import com.bank.customer.domain.model.Customer;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CustomerPersistenceAdapter implements CustomerRepository {

    private static final String IDENTIFICATION_CONSTRAINT = "uk_person_identification";
    private static final Sort BY_NAME = Sort.by("name").and(Sort.by("id"));

    private final CustomerJpaRepository customerJpaRepository;
    private final CustomerEntityMapper customerEntityMapper;

    @Override
    public Customer save(Customer customer) {
        CustomerEntity entity = customer.getId() == null
                ? customerEntityMapper.toNewEntity(customer)
                : loadForUpdate(customer);
        try {
            return customerEntityMapper.toDomain(customerJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (violates(exception, IDENTIFICATION_CONSTRAINT)) {
                throw new DuplicateIdentificationException();
            }
            throw exception;
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentCustomerChangeException();
        }
    }

    @Override
    public Optional<Customer> findByCustomerId(UUID customerId) {
        return customerJpaRepository.findByCustomerId(customerId).map(customerEntityMapper::toDomain);
    }

    @Override
    public PageResult<Customer> findAll(int page, int size) {
        Page<CustomerEntity> result = customerJpaRepository.findAll(PageRequest.of(page, size, BY_NAME));
        return new PageResult<>(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages())
                .map(customerEntityMapper::toDomain);
    }

    @Override
    public boolean existsByIdentification(String identification) {
        return customerJpaRepository.existsByIdentification(identification);
    }

    @Override
    public boolean existsByIdentificationForOtherCustomer(String identification, UUID customerId) {
        return customerJpaRepository.existsByIdentificationAndCustomerIdNot(identification, customerId);
    }

    @Override
    public void delete(Customer customer) {
        CustomerEntity entity = customerJpaRepository.findById(customer.getId())
                .orElseThrow(() -> new CustomerNotFoundException(customer.getCustomerId()));
        try {
            customerJpaRepository.delete(entity);
            customerJpaRepository.flush();
        } catch (ObjectOptimisticLockingFailureException exception) {
            throw new ConcurrentCustomerChangeException();
        }
    }

    private CustomerEntity loadForUpdate(Customer customer) {
        CustomerEntity entity = customerJpaRepository.findById(customer.getId())
                .orElseThrow(() -> new CustomerNotFoundException(customer.getCustomerId()));
        customerEntityMapper.updateEntity(customer, entity);
        return entity;
    }

    private static boolean violates(DataIntegrityViolationException exception, String constraintName) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraintName.equalsIgnoreCase(violation.getConstraintName());
            }
            cause = cause.getCause();
        }
        return false;
    }
}

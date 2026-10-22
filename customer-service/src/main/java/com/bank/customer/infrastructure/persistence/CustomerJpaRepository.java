package com.bank.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByCustomerId(UUID customerId);

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationAndCustomerIdNot(String identification, UUID customerId);
}

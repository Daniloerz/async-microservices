package com.bank.customer.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CustomerJpaRepository extends JpaRepository<CustomerEntity, Long> {

    Optional<CustomerEntity> findByCustomerId(UUID customerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT customer FROM CustomerEntity customer WHERE customer.customerId = :customerId")
    Optional<CustomerEntity> findWithWriteLock(@Param("customerId") UUID customerId);

    boolean existsByIdentification(String identification);

    boolean existsByIdentificationAndCustomerIdNot(String identification, UUID customerId);
}

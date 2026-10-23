package com.bank.account.infrastructure.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface AccountJpaRepository extends JpaRepository<AccountEntity, Long> {

    Optional<AccountEntity> findByAccountNumber(String accountNumber);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT account FROM AccountEntity account WHERE account.accountNumber = :accountNumber")
    Optional<AccountEntity> findWithWriteLock(@Param("accountNumber") String accountNumber);

    boolean existsByAccountNumber(String accountNumber);

    Page<AccountEntity> findAllByCustomerId(UUID customerId, Pageable pageable);

    List<AccountEntity> findAllByCustomerIdAndCreatedAtBeforeOrderByAccountNumberAsc(UUID customerId, Instant before);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE account
               SET active = false, updated_at = now()
             WHERE customer_id = :customerId AND active
            """, nativeQuery = true)
    int deactivateAllByCustomerId(@Param("customerId") UUID customerId);
}

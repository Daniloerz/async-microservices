package com.bank.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface CustomerSnapshotJpaRepository extends JpaRepository<CustomerSnapshotEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_READ)
    @Query("SELECT snapshot FROM CustomerSnapshotEntity snapshot WHERE snapshot.customerId = :customerId")
    Optional<CustomerSnapshotEntity> findWithSharedLock(@Param("customerId") UUID customerId);
}

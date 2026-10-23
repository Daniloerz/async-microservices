package com.bank.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {

    Optional<MovementEntity> findByMovementId(UUID movementId);

    Optional<MovementEntity> findFirstByAccountNumberOrderByIdDesc(String accountNumber);

    Page<MovementEntity> findAllByAccountNumber(String accountNumber, Pageable pageable);
}

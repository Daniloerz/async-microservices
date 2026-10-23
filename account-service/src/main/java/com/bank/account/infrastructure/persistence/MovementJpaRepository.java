package com.bank.account.infrastructure.persistence;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovementJpaRepository extends JpaRepository<MovementEntity, Long> {

    Optional<MovementEntity> findByMovementId(UUID movementId);

    Optional<MovementEntity> findFirstByAccountNumberOrderByIdDesc(String accountNumber);

    Page<MovementEntity> findAllByAccountNumber(String accountNumber, Pageable pageable);

    @Query("""
            SELECT movement FROM MovementEntity movement
             WHERE movement.accountNumber IN :accountNumbers
               AND movement.movementDate >= :fromInclusive
               AND movement.movementDate < :toExclusive
             ORDER BY movement.id
            """)
    List<MovementEntity> findOfAccountsBetween(@Param("accountNumbers") Collection<String> accountNumbers,
            @Param("fromInclusive") Instant fromInclusive, @Param("toExclusive") Instant toExclusive);

    Optional<MovementEntity> findFirstByAccountNumberAndMovementDateBeforeOrderByMovementDateDescIdDesc(
            String accountNumber, Instant before);
}

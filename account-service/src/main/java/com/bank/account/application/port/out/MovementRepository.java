package com.bank.account.application.port.out;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.bank.account.domain.model.Movement;

public interface MovementRepository {

    Movement save(Movement movement);

    Optional<Movement> findByMovementId(UUID movementId);

    Optional<Movement> findLastOfAccount(String accountNumber);

    PageResult<Movement> findAll(String accountNumber, int page, int size);

    List<Movement> findOfAccountsBetween(Collection<String> accountNumbers, Instant fromInclusive, Instant toExclusive);

    Optional<Movement> findLastBefore(String accountNumber, Instant before);
}

package com.bank.account.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.bank.account.domain.model.Movement;

public interface MovementRepository {

    Movement save(Movement movement);

    Optional<Movement> findByMovementId(UUID movementId);

    Optional<Movement> findLastOfAccount(String accountNumber);

    PageResult<Movement> findAll(String accountNumber, int page, int size);
}

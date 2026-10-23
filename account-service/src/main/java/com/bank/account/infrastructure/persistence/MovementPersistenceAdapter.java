package com.bank.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class MovementPersistenceAdapter implements MovementRepository {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "id");

    private final MovementJpaRepository movementJpaRepository;
    private final MovementEntityMapper movementEntityMapper;

    @Override
    public Movement save(Movement movement) {
        MovementEntity entity = movement.getId() == null
                ? movementEntityMapper.toNewEntity(movement)
                : loadForUpdate(movement);
        return movementEntityMapper.toDomain(movementJpaRepository.saveAndFlush(entity));
    }

    @Override
    public Optional<Movement> findByMovementId(UUID movementId) {
        return movementJpaRepository.findByMovementId(movementId).map(movementEntityMapper::toDomain);
    }

    @Override
    public Optional<Movement> findLastOfAccount(String accountNumber) {
        return movementJpaRepository.findFirstByAccountNumberOrderByIdDesc(accountNumber)
                .map(movementEntityMapper::toDomain);
    }

    @Override
    public PageResult<Movement> findAll(String accountNumber, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, NEWEST_FIRST);
        Page<MovementEntity> result = accountNumber == null
                ? movementJpaRepository.findAll(pageable)
                : movementJpaRepository.findAllByAccountNumber(accountNumber, pageable);
        return new PageResult<>(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages())
                .map(movementEntityMapper::toDomain);
    }

    private MovementEntity loadForUpdate(Movement movement) {
        MovementEntity entity = movementJpaRepository.findById(movement.getId())
                .orElseThrow(() -> new MovementNotFoundException(movement.getMovementId()));
        movementEntityMapper.updateEntity(movement, entity);
        return entity;
    }
}

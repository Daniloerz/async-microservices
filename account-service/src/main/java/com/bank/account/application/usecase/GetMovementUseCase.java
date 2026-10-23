package com.bank.account.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetMovementUseCase {

    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    public Movement get(UUID movementId) {
        return movementRepository.findByMovementId(movementId)
                .orElseThrow(() -> new MovementNotFoundException(movementId));
    }
}

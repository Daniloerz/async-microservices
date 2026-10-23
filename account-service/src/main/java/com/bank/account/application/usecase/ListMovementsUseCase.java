package com.bank.account.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListMovementsUseCase {

    private final MovementRepository movementRepository;

    @Transactional(readOnly = true)
    public PageResult<Movement> list(String accountNumber, int page, int size) {
        return movementRepository.findAll(accountNumber, page, size);
    }
}

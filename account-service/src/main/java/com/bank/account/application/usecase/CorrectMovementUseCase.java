package com.bank.account.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.MovementNotCorrectableException;
import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CorrectMovementUseCase {

    private final MovementRepository movementRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Movement correct(UUID movementId, BigDecimal newAmount) {
        String accountNumber = movementRepository.findByMovementId(movementId)
                .map(Movement::getAccountNumber)
                .orElseThrow(() -> new MovementNotFoundException(movementId));

        Account account = accountRepository.findForUpdate(accountNumber)
                .orElseThrow(() -> new IllegalStateException("Movement account is missing"));
        Movement lastMovement = movementRepository.findLastOfAccount(accountNumber)
                .filter(last -> last.getMovementId().equals(movementId))
                .orElseThrow(MovementNotCorrectableException::new);

        account.correctMovement(lastMovement, newAmount);
        accountRepository.save(account);
        return movementRepository.save(lastMovement);
    }
}

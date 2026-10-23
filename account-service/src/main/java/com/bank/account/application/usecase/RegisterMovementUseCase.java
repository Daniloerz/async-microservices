package com.bank.account.application.usecase;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.AccountNotAvailableException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RegisterMovementUseCase {

    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;

    @Transactional
    public Movement register(String accountNumber, BigDecimal amount) {
        Account account = accountRepository.findForUpdate(accountNumber)
                .orElseThrow(AccountNotAvailableException::new);
        Movement movement = account.registerMovement(amount);
        accountRepository.save(account);
        return movementRepository.save(movement);
    }
}

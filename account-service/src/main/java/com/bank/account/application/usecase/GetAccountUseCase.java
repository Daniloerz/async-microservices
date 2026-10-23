package com.bank.account.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.domain.model.Account;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GetAccountUseCase {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public Account get(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
    }
}

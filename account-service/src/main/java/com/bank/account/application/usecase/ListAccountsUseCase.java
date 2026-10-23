package com.bank.account.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Account;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ListAccountsUseCase {

    private final AccountRepository accountRepository;

    @Transactional(readOnly = true)
    public PageResult<Account> list(UUID customerId, int page, int size) {
        return accountRepository.findAll(customerId, page, size);
    }
}

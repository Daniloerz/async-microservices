package com.bank.account.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountChanges;
import com.bank.account.domain.model.CustomerSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UpdateAccountUseCase {

    private final AccountRepository accountRepository;
    private final CustomerSnapshotRepository customerSnapshotRepository;

    @Transactional
    public Account update(String accountNumber, AccountChanges changes) {
        Account account = accountRepository.findForUpdate(accountNumber)
                .orElseThrow(() -> new AccountNotFoundException(accountNumber));
        CustomerSnapshot owner = customerSnapshotRepository.findById(account.getCustomerId())
                .orElseThrow(() -> new IllegalStateException("Account owner is missing from the customer projection"));

        account.apply(changes, owner);
        return accountRepository.save(account);
    }
}

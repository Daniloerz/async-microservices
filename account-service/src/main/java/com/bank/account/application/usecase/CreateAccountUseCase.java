package com.bank.account.application.usecase;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.AccountType;
import com.bank.account.domain.model.CustomerSnapshot;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateAccountUseCase {

    private final AccountRepository accountRepository;
    private final CustomerSnapshotRepository customerSnapshotRepository;

    @Transactional
    public Account create(String accountNumber, AccountType accountType, BigDecimal initialBalance,
            UUID customerId, boolean active) {
        CustomerSnapshot owner = customerSnapshotRepository.findForAccountOpening(customerId)
                .orElseThrow(CustomerNotAvailableException::unknown);
        if (!owner.active()) {
            throw CustomerNotAvailableException.inactive();
        }
        if (accountRepository.existsByAccountNumber(accountNumber)) {
            throw new DuplicateAccountNumberException();
        }
        return accountRepository.save(Account.open(accountNumber, accountType, initialBalance, customerId, active));
    }
}

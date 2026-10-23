package com.bank.account.infrastructure.persistence;

import java.util.Optional;
import java.util.UUID;

import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.PageResult;
import com.bank.account.domain.model.Account;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class AccountPersistenceAdapter implements AccountRepository {

    private static final String ACCOUNT_NUMBER_CONSTRAINT = "uk_account_account_number";
    private static final String CUSTOMER_CONSTRAINT = "fk_account_customer_snapshot";
    private static final Sort BY_ACCOUNT_NUMBER = Sort.by("accountNumber");

    private final AccountJpaRepository accountJpaRepository;
    private final AccountEntityMapper accountEntityMapper;

    @Override
    public Account save(Account account) {
        AccountEntity entity = account.getId() == null
                ? accountEntityMapper.toNewEntity(account)
                : loadForUpdate(account);
        try {
            return accountEntityMapper.toDomain(accountJpaRepository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            String constraint = violatedConstraint(exception);
            if (ACCOUNT_NUMBER_CONSTRAINT.equalsIgnoreCase(constraint)) {
                throw new DuplicateAccountNumberException();
            }
            if (CUSTOMER_CONSTRAINT.equalsIgnoreCase(constraint)) {
                throw CustomerNotAvailableException.unknown();
            }
            throw exception;
        }
    }

    @Override
    public Optional<Account> findByAccountNumber(String accountNumber) {
        return accountJpaRepository.findByAccountNumber(accountNumber).map(accountEntityMapper::toDomain);
    }

    @Override
    public Optional<Account> findForUpdate(String accountNumber) {
        return accountJpaRepository.findWithWriteLock(accountNumber).map(accountEntityMapper::toDomain);
    }

    @Override
    public PageResult<Account> findAll(UUID customerId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, BY_ACCOUNT_NUMBER);
        Page<AccountEntity> result = customerId == null
                ? accountJpaRepository.findAll(pageable)
                : accountJpaRepository.findAllByCustomerId(customerId, pageable);
        return new PageResult<>(result.getContent(), page, size, result.getTotalElements(), result.getTotalPages())
                .map(accountEntityMapper::toDomain);
    }

    @Override
    public boolean existsByAccountNumber(String accountNumber) {
        return accountJpaRepository.existsByAccountNumber(accountNumber);
    }

    @Override
    public int deactivateAllOfCustomer(UUID customerId) {
        return accountJpaRepository.deactivateAllByCustomerId(customerId);
    }

    private AccountEntity loadForUpdate(Account account) {
        AccountEntity entity = accountJpaRepository.findById(account.getId())
                .orElseThrow(() -> new AccountNotFoundException(account.getAccountNumber()));
        accountEntityMapper.updateEntity(account, entity);
        return entity;
    }

    private static String violatedConstraint(DataIntegrityViolationException exception) {
        Throwable cause = exception;
        while (cause != null) {
            if (cause instanceof ConstraintViolationException violation) {
                return violation.getConstraintName();
            }
            cause = cause.getCause();
        }
        return null;
    }
}

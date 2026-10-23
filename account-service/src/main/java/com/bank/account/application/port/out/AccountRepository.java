package com.bank.account.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.bank.account.domain.model.Account;

public interface AccountRepository {

    Account save(Account account);

    Optional<Account> findByAccountNumber(String accountNumber);

    Optional<Account> findForUpdate(String accountNumber);

    PageResult<Account> findAll(UUID customerId, int page, int size);

    boolean existsByAccountNumber(String accountNumber);

    int deactivateAllOfCustomer(UUID customerId);
}

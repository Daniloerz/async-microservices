package com.bank.account.application.usecase;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import com.bank.account.application.exception.CustomerNotFoundException;
import com.bank.account.application.port.out.AccountRepository;
import com.bank.account.application.port.out.CustomerSnapshotRepository;
import com.bank.account.application.port.out.MovementRepository;
import com.bank.account.application.report.AccountStatement;
import com.bank.account.application.report.AccountStatementAccount;
import com.bank.account.application.report.ReportPeriod;
import com.bank.account.application.report.ReportSettings;
import com.bank.account.domain.model.Account;
import com.bank.account.domain.model.CustomerSnapshot;
import com.bank.account.domain.model.Movement;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenerateAccountStatementUseCase {

    private final CustomerSnapshotRepository customerSnapshotRepository;
    private final AccountRepository accountRepository;
    private final MovementRepository movementRepository;
    private final ReportSettings reportSettings;

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AccountStatement generate(UUID customerId, LocalDate startDate, LocalDate endDate) {
        ReportPeriod period = ReportPeriod.of(startDate, endDate, reportSettings.zone(), reportSettings.maxRangeDays());
        CustomerSnapshot customer = customerSnapshotRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(customerId));

        List<Account> accounts = accountRepository.findAllOfCustomerOpenedBefore(customerId, period.toExclusive());
        Map<String, List<Movement>> movementsByAccount = movementsOf(accounts, period);

        List<AccountStatementAccount> lines = accounts.stream()
                .map(account -> AccountStatementAccount.from(account, openingBalance(account, period),
                        movementsByAccount.getOrDefault(account.getAccountNumber(), List.of())))
                .toList();
        return new AccountStatement(customer, period, lines);
    }

    private Map<String, List<Movement>> movementsOf(List<Account> accounts, ReportPeriod period) {
        if (accounts.isEmpty()) {
            return Map.of();
        }
        List<String> accountNumbers = accounts.stream().map(Account::getAccountNumber).toList();
        return movementRepository.findOfAccountsBetween(accountNumbers, period.fromInclusive(), period.toExclusive())
                .stream()
                .collect(Collectors.groupingBy(Movement::getAccountNumber));
    }

    private BigDecimal openingBalance(Account account, ReportPeriod period) {
        return movementRepository.findLastBefore(account.getAccountNumber(), period.fromInclusive())
                .map(Movement::getBalance)
                .orElse(account.getInitialBalance());
    }
}

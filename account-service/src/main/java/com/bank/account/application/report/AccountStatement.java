package com.bank.account.application.report;

import java.util.List;
import java.util.Objects;

import com.bank.account.domain.model.CustomerSnapshot;

public record AccountStatement(CustomerSnapshot customer, ReportPeriod period, List<AccountStatementAccount> accounts) {

    public AccountStatement {
        Objects.requireNonNull(customer, "customer");
        Objects.requireNonNull(period, "period");
        accounts = accounts == null ? List.of() : List.copyOf(accounts);
    }
}

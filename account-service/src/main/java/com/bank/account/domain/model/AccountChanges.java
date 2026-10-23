package com.bank.account.domain.model;

public record AccountChanges(AccountType accountType, Boolean active) {

    public boolean isEmpty() {
        return accountType == null && active == null;
    }
}

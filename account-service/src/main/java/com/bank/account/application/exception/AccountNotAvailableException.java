package com.bank.account.application.exception;

public class AccountNotAvailableException extends RuntimeException {

    public AccountNotAvailableException() {
        super("Account does not exist");
    }
}

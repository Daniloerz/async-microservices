package com.bank.account.application.exception;

public class DuplicateAccountNumberException extends RuntimeException {

    public DuplicateAccountNumberException() {
        super("An account with this number already exists");
    }
}

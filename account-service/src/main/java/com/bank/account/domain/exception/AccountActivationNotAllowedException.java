package com.bank.account.domain.exception;

public class AccountActivationNotAllowedException extends RuntimeException {

    public AccountActivationNotAllowedException() {
        super("An account cannot be activated while its customer is not active");
    }
}

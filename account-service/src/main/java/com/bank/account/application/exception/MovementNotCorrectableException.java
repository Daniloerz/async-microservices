package com.bank.account.application.exception;

public class MovementNotCorrectableException extends RuntimeException {

    public MovementNotCorrectableException() {
        super("Only the last movement of an account can be corrected");
    }
}

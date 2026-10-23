package com.bank.account.infrastructure.messaging;

public class InvalidCustomerEventException extends RuntimeException {

    public InvalidCustomerEventException(String message) {
        super(message);
    }

    public InvalidCustomerEventException(String message, Throwable cause) {
        super(message, cause);
    }
}

package com.bank.customer.application.exception;

public class ConcurrentCustomerChangeException extends RuntimeException {

    public ConcurrentCustomerChangeException() {
        super("The customer was changed or deleted by another request, please retry");
    }
}

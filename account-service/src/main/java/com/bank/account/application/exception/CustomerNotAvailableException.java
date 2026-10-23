package com.bank.account.application.exception;

public class CustomerNotAvailableException extends RuntimeException {

    private CustomerNotAvailableException(String message) {
        super(message);
    }

    public static CustomerNotAvailableException unknown() {
        return new CustomerNotAvailableException("Customer does not exist or is not yet known by this service");
    }

    public static CustomerNotAvailableException inactive() {
        return new CustomerNotAvailableException("Customer is not active");
    }
}

package com.bank.customer.application.exception;

public class DuplicateIdentificationException extends RuntimeException {

    public DuplicateIdentificationException() {
        super("A customer with this identification already exists");
    }
}

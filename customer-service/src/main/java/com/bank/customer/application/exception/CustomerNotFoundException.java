package com.bank.customer.application.exception;

import java.util.UUID;

import lombok.Getter;

@Getter
public class CustomerNotFoundException extends RuntimeException {

    private final UUID customerId;

    public CustomerNotFoundException(UUID customerId) {
        super("Customer not found");
        this.customerId = customerId;
    }
}

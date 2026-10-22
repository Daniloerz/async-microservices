package com.bank.customer.application.port.out;

public interface PasswordHasher {

    String hash(String rawPassword);
}

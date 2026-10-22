package com.bank.customer.domain.model;

import java.util.Objects;

public record CustomerDetails(
        String name,
        Gender gender,
        int age,
        String identification,
        String address,
        String phone) {

    public CustomerDetails {
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(gender, "gender");
        Objects.requireNonNull(identification, "identification");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(phone, "phone");
    }
}

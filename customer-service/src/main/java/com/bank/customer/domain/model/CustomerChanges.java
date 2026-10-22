package com.bank.customer.domain.model;

import java.util.stream.Stream;

public record CustomerChanges(
        String name,
        Gender gender,
        Integer age,
        String identification,
        String address,
        String phone,
        Boolean active) {

    public boolean isEmpty() {
        return Stream.of(name, gender, age, identification, address, phone, active).allMatch(value -> value == null);
    }
}

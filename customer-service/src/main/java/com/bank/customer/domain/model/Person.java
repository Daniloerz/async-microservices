package com.bank.customer.domain.model;

import java.util.Objects;

import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public abstract class Person {

    private String name;
    private Gender gender;
    private int age;
    private String identification;
    private String address;
    private String phone;

    protected Person(String name, Gender gender, int age, String identification, String address, String phone) {
        this.name = Objects.requireNonNull(name, "name");
        this.gender = Objects.requireNonNull(gender, "gender");
        this.age = age;
        this.identification = Objects.requireNonNull(identification, "identification");
        this.address = Objects.requireNonNull(address, "address");
        this.phone = Objects.requireNonNull(phone, "phone");
    }

    protected void updatePersonalData(String name, Gender gender, Integer age, String identification,
            String address, String phone) {
        this.name = Objects.requireNonNullElse(name, this.name);
        this.gender = Objects.requireNonNullElse(gender, this.gender);
        this.age = Objects.requireNonNullElse(age, this.age);
        this.identification = Objects.requireNonNullElse(identification, this.identification);
        this.address = Objects.requireNonNullElse(address, this.address);
        this.phone = Objects.requireNonNullElse(phone, this.phone);
    }
}

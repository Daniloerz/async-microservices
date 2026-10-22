package com.bank.customer.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString(callSuper = true)
public class Customer extends Person {

    private final Long id;
    private final UUID customerId;
    @ToString.Exclude
    private String passwordHash;
    private boolean active;
    private final Instant createdAt;
    private final Instant updatedAt;

    @Builder
    private Customer(Long id, UUID customerId, String name, Gender gender, int age, String identification,
            String address, String phone, String passwordHash, boolean active, Instant createdAt, Instant updatedAt) {
        super(name, gender, age, identification, address, phone);
        this.id = id;
        this.customerId = Objects.requireNonNull(customerId, "customerId");
        this.passwordHash = Objects.requireNonNull(passwordHash, "passwordHash");
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Customer create(CustomerDetails details, String passwordHash, boolean active) {
        return Customer.builder()
                .customerId(UUID.randomUUID())
                .name(details.name())
                .gender(details.gender())
                .age(details.age())
                .identification(details.identification())
                .address(details.address())
                .phone(details.phone())
                .passwordHash(passwordHash)
                .active(active)
                .build();
    }

    public void apply(CustomerChanges changes) {
        updatePersonalData(changes.name(), changes.gender(), changes.age(), changes.identification(),
                changes.address(), changes.phone());
        if (changes.active() != null) {
            this.active = changes.active();
        }
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = Objects.requireNonNull(newPasswordHash, "newPasswordHash");
    }
}

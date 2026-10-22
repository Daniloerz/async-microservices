package com.bank.customer.interfaces.rest.dto;

import java.time.Instant;
import java.util.UUID;

import com.bank.customer.domain.model.Gender;

public record CustomerResponse(
        UUID customerId,
        String name,
        Gender gender,
        int age,
        String identification,
        String address,
        String phone,
        boolean active,
        Instant createdAt,
        Instant updatedAt) {
}

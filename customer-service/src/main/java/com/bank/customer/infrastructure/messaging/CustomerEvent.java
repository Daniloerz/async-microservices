package com.bank.customer.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import com.bank.customer.application.port.out.CustomerEventType;

public record CustomerEvent(
        UUID eventId,
        CustomerEventType eventType,
        int version,
        Instant occurredAt,
        CustomerData customer) {

    public record CustomerData(UUID customerId, String name, boolean active) {
    }
}

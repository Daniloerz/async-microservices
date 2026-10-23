package com.bank.account.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record CustomerEventMessage(
        UUID eventId,
        String eventType,
        Integer version,
        Instant occurredAt,
        CustomerData customer) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record CustomerData(UUID customerId, String name, Boolean active) {
    }
}

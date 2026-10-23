package com.bank.account.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerSnapshot(UUID customerId, String name, boolean active, Instant lastEventAt) {

    public CustomerSnapshot {
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(lastEventAt, "lastEventAt");
    }

    public boolean acceptsEventAt(Instant occurredAt) {
        return !occurredAt.isBefore(lastEventAt);
    }
}

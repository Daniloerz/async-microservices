package com.bank.account.application.command;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record CustomerChangeCommand(
        UUID eventId,
        CustomerChangeType changeType,
        Instant occurredAt,
        UUID customerId,
        String name,
        boolean active) {

    public CustomerChangeCommand {
        Objects.requireNonNull(eventId, "eventId");
        Objects.requireNonNull(changeType, "changeType");
        Objects.requireNonNull(occurredAt, "occurredAt");
        Objects.requireNonNull(customerId, "customerId");
        Objects.requireNonNull(name, "name");
    }

    public boolean isDeletion() {
        return changeType == CustomerChangeType.DELETED;
    }
}

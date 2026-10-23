package com.bank.account.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class CustomerSnapshotTest {

    private static final Instant LAST_EVENT_AT = Instant.parse("2026-10-22T10:00:00Z");

    private final CustomerSnapshot snapshot = new CustomerSnapshot(UUID.randomUUID(), "Jose Lema", true, LAST_EVENT_AT);

    @Test
    void acceptsEventsThatAreNotOlderThanTheLastAppliedOne() {
        assertThat(snapshot.acceptsEventAt(LAST_EVENT_AT.plusMillis(1))).isTrue();
        assertThat(snapshot.acceptsEventAt(LAST_EVENT_AT)).isTrue();
    }

    @Test
    void rejectsEventsOlderThanTheLastAppliedOne() {
        assertThat(snapshot.acceptsEventAt(LAST_EVENT_AT.minusMillis(1))).isFalse();
    }
}

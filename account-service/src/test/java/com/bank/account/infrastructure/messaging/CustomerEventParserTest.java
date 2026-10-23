package com.bank.account.infrastructure.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.bank.account.application.command.CustomerChangeCommand;
import com.bank.account.application.command.CustomerChangeType;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.json.JsonMapper;

class CustomerEventParserTest {

    private static final String EVENT_ID = "8a3a3f8e-5c2b-4a8e-9d6f-1b2c3d4e5f60";
    private static final String CUSTOMER_ID = "3f6c2a0e-8f1d-4c55-9a51-0d1f3e7b9a10";

    private CustomerEventParser parser;

    @BeforeEach
    void setUp() {
        JsonMapper strictMapper = JsonMapper.builder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .build();
        parser = new CustomerEventParser(strictMapper);
    }

    @Test
    void validEventIsTranslatedIntoACommand() {
        CustomerChangeCommand command = parser.parse(event("CUSTOMER_DELETED", 1));

        assertThat(command.eventId()).isEqualTo(UUID.fromString(EVENT_ID));
        assertThat(command.changeType()).isEqualTo(CustomerChangeType.DELETED);
        assertThat(command.occurredAt()).isEqualTo(Instant.parse("2026-10-22T21:00:00Z"));
        assertThat(command.customerId()).isEqualTo(UUID.fromString(CUSTOMER_ID));
        assertThat(command.name()).isEqualTo("Jose Lema");
        assertThat(command.active()).isTrue();
    }

    @Test
    void unknownExtraFieldsAreTolerated() {
        String withExtraFields = event("CUSTOMER_CREATED", 1)
                .replace("\"version\": 1", "\"version\": 1, \"source\": \"customer-service\"")
                .replace("\"active\": true", "\"active\": true, \"segment\": \"VIP\"");

        assertThat(parser.parse(withExtraFields).changeType()).isEqualTo(CustomerChangeType.CREATED);
    }

    @Test
    void malformedJsonIsRejected() {
        assertThatThrownBy(() -> parser.parse("{\"eventId\": "))
                .isInstanceOf(InvalidCustomerEventException.class);
    }

    @Test
    void emptyPayloadIsRejected() {
        assertThatThrownBy(() -> parser.parse(" "))
                .isInstanceOf(InvalidCustomerEventException.class);
    }

    @Test
    void unsupportedVersionIsRejected() {
        assertThatThrownBy(() -> parser.parse(event("CUSTOMER_CREATED", 2)))
                .isInstanceOf(InvalidCustomerEventException.class)
                .hasMessage("Unsupported customer event version: 2");
    }

    @Test
    void unknownEventTypeIsRejected() {
        assertThatThrownBy(() -> parser.parse(event("CUSTOMER_MERGED", 1)))
                .isInstanceOf(InvalidCustomerEventException.class)
                .hasMessage("Unknown customer event type");
    }

    @Test
    void missingRequiredFieldIsRejected() {
        String withoutName = event("CUSTOMER_CREATED", 1).replace("\"name\": \"Jose Lema\",", "");

        assertThatThrownBy(() -> parser.parse(withoutName))
                .isInstanceOf(InvalidCustomerEventException.class)
                .hasMessage("Customer event is missing required fields");
    }

    @Test
    void invalidUuidIsRejected() {
        String invalidId = event("CUSTOMER_CREATED", 1).replace(EVENT_ID, "not-a-uuid");

        assertThatThrownBy(() -> parser.parse(invalidId))
                .isInstanceOf(InvalidCustomerEventException.class);
    }

    private static String event(String eventType, int version) {
        return """
                {
                  "eventId": "%s",
                  "eventType": "%s",
                  "version": %d,
                  "occurredAt": "2026-10-22T21:00:00Z",
                  "customer": {
                    "customerId": "%s",
                    "name": "Jose Lema",
                    "active": true
                  }
                }
                """.formatted(EVENT_ID, eventType, version, CUSTOMER_ID);
    }
}

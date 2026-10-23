package com.bank.account.infrastructure.messaging;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.bank.account.application.command.CustomerChangeCommand;
import com.bank.account.application.command.CustomerChangeType;

import lombok.RequiredArgsConstructor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class CustomerEventParser {

    static final int SUPPORTED_VERSION = 1;

    private static final Map<String, CustomerChangeType> CHANGE_TYPES = Map.of(
            "CUSTOMER_CREATED", CustomerChangeType.CREATED,
            "CUSTOMER_UPDATED", CustomerChangeType.UPDATED,
            "CUSTOMER_DELETED", CustomerChangeType.DELETED);

    private final JsonMapper jsonMapper;

    public CustomerChangeCommand parse(String payload) {
        CustomerEventMessage message = read(payload);

        if (message.version() == null || message.version() != SUPPORTED_VERSION) {
            throw new InvalidCustomerEventException("Unsupported customer event version: " + message.version());
        }
        CustomerChangeType changeType = Optional.ofNullable(message.eventType())
                .map(CHANGE_TYPES::get)
                .orElseThrow(() -> new InvalidCustomerEventException("Unknown customer event type"));
        CustomerEventMessage.CustomerData customer = Optional.ofNullable(message.customer())
                .orElseThrow(() -> new InvalidCustomerEventException("Customer data is missing"));

        if (message.eventId() == null || message.occurredAt() == null || customer.customerId() == null
                || customer.name() == null || customer.active() == null) {
            throw new InvalidCustomerEventException("Customer event is missing required fields");
        }

        return new CustomerChangeCommand(message.eventId(), changeType, message.occurredAt(),
                customer.customerId(), customer.name(), customer.active());
    }

    private CustomerEventMessage read(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new InvalidCustomerEventException("Customer event is empty");
        }
        try {
            return jsonMapper.readValue(payload, CustomerEventMessage.class);
        } catch (JacksonException exception) {
            throw new InvalidCustomerEventException("Customer event is not valid JSON for the contract", exception);
        }
    }
}

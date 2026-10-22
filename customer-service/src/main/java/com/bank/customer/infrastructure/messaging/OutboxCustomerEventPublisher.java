package com.bank.customer.infrastructure.messaging;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.bank.customer.application.port.out.CustomerEventPublisher;
import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.infrastructure.persistence.OutboxEventEntity;
import com.bank.customer.infrastructure.persistence.OutboxEventJpaRepository;

import lombok.RequiredArgsConstructor;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class OutboxCustomerEventPublisher implements CustomerEventPublisher {

    static final int EVENT_VERSION = 1;

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final JsonMapper jsonMapper;

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(CustomerEventType eventType, Customer customer) {
        boolean active = eventType != CustomerEventType.CUSTOMER_DELETED && customer.isActive();
        CustomerEvent event = new CustomerEvent(
                UUID.randomUUID(),
                eventType,
                EVENT_VERSION,
                Instant.now(),
                new CustomerEvent.CustomerData(customer.getCustomerId(), customer.getName(), active));

        outboxEventJpaRepository.save(new OutboxEventEntity(
                event.eventId(), customer.getCustomerId(), eventType, jsonMapper.writeValueAsString(event)));
    }
}

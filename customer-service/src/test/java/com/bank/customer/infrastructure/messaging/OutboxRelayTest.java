package com.bank.customer.infrastructure.messaging;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.kafka.common.errors.TimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Limit;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.test.util.ReflectionTestUtils;

import com.bank.customer.application.port.out.CustomerEventType;
import com.bank.customer.infrastructure.config.CustomerEventsTopicProperties;
import com.bank.customer.infrastructure.config.OutboxProperties;
import com.bank.customer.infrastructure.persistence.OutboxEventEntity;
import com.bank.customer.infrastructure.persistence.OutboxEventJpaRepository;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final String TOPIC = "customer-events";
    private static final int BATCH_SIZE = 100;

    @Mock
    private OutboxEventJpaRepository outboxEventJpaRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    private OutboxRelay outboxRelay;

    @BeforeEach
    void setUp() {
        outboxRelay = new OutboxRelay(outboxEventJpaRepository, kafkaTemplate,
                new OutboxProperties(1000, BATCH_SIZE, Duration.ofSeconds(1)),
                new CustomerEventsTopicProperties(TOPIC, 3, 1));
    }

    @Test
    void sentEventsAreMarkedAsPublishedInOrder() {
        OutboxEventEntity first = outboxEvent(1L);
        OutboxEventEntity second = outboxEvent(2L);
        when(outboxEventJpaRepository.findByPublishedAtIsNullOrderByIdAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(first, second));
        when(kafkaTemplate.send(anyString(), anyString(), anyString())).thenReturn(succeeded());

        outboxRelay.publishPendingEvents();

        InOrder order = Mockito.inOrder(kafkaTemplate, outboxEventJpaRepository);
        order.verify(kafkaTemplate).send(TOPIC, first.getAggregateId().toString(), first.getPayload());
        order.verify(outboxEventJpaRepository).markPublished(1L);
        order.verify(kafkaTemplate).send(TOPIC, second.getAggregateId().toString(), second.getPayload());
        order.verify(outboxEventJpaRepository).markPublished(2L);
    }

    @Test
    void relayStopsAtTheFirstFailureSoLaterEventsDoNotOvertakeIt() {
        OutboxEventEntity first = outboxEvent(1L);
        OutboxEventEntity second = outboxEvent(2L);
        when(outboxEventJpaRepository.findByPublishedAtIsNullOrderByIdAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(first, second));
        when(kafkaTemplate.send(TOPIC, first.getAggregateId().toString(), first.getPayload()))
                .thenReturn(CompletableFuture.failedFuture(new TimeoutException("broker not available")));

        outboxRelay.publishPendingEvents();

        verify(outboxEventJpaRepository, never()).markPublished(anyLong());
        verify(kafkaTemplate, never()).send(TOPIC, second.getAggregateId().toString(), second.getPayload());
    }

    @Test
    void synchronousSendFailureKeepsTheEventPendingWithoutPropagating() {
        OutboxEventEntity event = outboxEvent(1L);
        when(outboxEventJpaRepository.findByPublishedAtIsNullOrderByIdAsc(Limit.of(BATCH_SIZE)))
                .thenReturn(List.of(event));
        when(kafkaTemplate.send(anyString(), anyString(), anyString()))
                .thenThrow(new KafkaException("Send failed"));

        outboxRelay.publishPendingEvents();

        verify(outboxEventJpaRepository, never()).markPublished(anyLong());
    }

    @Test
    void nothingIsSentWhenThereAreNoPendingEvents() {
        when(outboxEventJpaRepository.findByPublishedAtIsNullOrderByIdAsc(any(Limit.class))).thenReturn(List.of());

        outboxRelay.publishPendingEvents();

        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    private static OutboxEventEntity outboxEvent(long id) {
        OutboxEventEntity event = new OutboxEventEntity(UUID.randomUUID(), UUID.randomUUID(),
                CustomerEventType.CUSTOMER_CREATED, "{\"id\":" + id + "}");
        ReflectionTestUtils.setField(event, "id", id);
        return event;
    }

    private static CompletableFuture<SendResult<String, String>> succeeded() {
        return CompletableFuture.completedFuture(null);
    }
}

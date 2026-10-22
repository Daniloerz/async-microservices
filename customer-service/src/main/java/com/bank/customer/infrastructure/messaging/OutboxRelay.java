package com.bank.customer.infrastructure.messaging;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.data.domain.Limit;
import org.springframework.kafka.KafkaException;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.bank.customer.infrastructure.config.CustomerEventsTopicProperties;
import com.bank.customer.infrastructure.config.OutboxProperties;
import com.bank.customer.infrastructure.persistence.OutboxEventEntity;
import com.bank.customer.infrastructure.persistence.OutboxEventJpaRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxRelay {

    private final OutboxEventJpaRepository outboxEventJpaRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final OutboxProperties outboxProperties;
    private final CustomerEventsTopicProperties topicProperties;

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay-ms}")
    public void publishPendingEvents() {
        List<OutboxEventEntity> pendingEvents =
                outboxEventJpaRepository.findByPublishedAtIsNullOrderByIdAsc(Limit.of(outboxProperties.batchSize()));

        for (OutboxEventEntity event : pendingEvents) {
            if (!send(event)) {
                return;
            }
            outboxEventJpaRepository.markPublished(event.getId());
        }
    }

    private boolean send(OutboxEventEntity event) {
        try {
            kafkaTemplate.send(topicProperties.topic(), event.getAggregateId().toString(), event.getPayload())
                    .get(outboxProperties.sendTimeout().toMillis(), TimeUnit.MILLISECONDS);
            return true;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while publishing outbox event {}", event.getEventId());
            return false;
        } catch (ExecutionException | TimeoutException | KafkaException exception) {
            log.warn("Could not publish outbox event {}, it will be retried: {}",
                    event.getEventId(), exception.toString());
            return false;
        }
    }
}

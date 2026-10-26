package com.bank.account.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;

import com.bank.account.infrastructure.messaging.InvalidCustomerEventException;

@Configuration
@EnableConfigurationProperties(CustomerEventsProperties.class)
public class KafkaConsumerConfig {

    private static final double BACK_OFF_MULTIPLIER = 2.0;

    @Bean
    NewTopic customerEventsTopic(CustomerEventsProperties properties) {
        return TopicBuilder.name(properties.topic())
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    @Bean
    NewTopic customerEventsDeadLetterTopic(CustomerEventsProperties properties) {
        return TopicBuilder.name(properties.deadLetterTopic())
                .partitions(properties.partitions())
                .replicas(properties.replicas())
                .build();
    }

    @Bean
    DefaultErrorHandler customerEventsErrorHandler(KafkaTemplate<?, ?> kafkaTemplate,
            CustomerEventsProperties properties) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
                (consumerRecord, exception) -> new TopicPartition(properties.deadLetterTopic(), consumerRecord.partition()));
        ExponentialBackOffWithMaxRetries backOff = new ExponentialBackOffWithMaxRetries(properties.retryAttempts());
        backOff.setInitialInterval(properties.retryInterval().toMillis());
        backOff.setMultiplier(BACK_OFF_MULTIPLIER);
        backOff.setMaxInterval(properties.retryMaxInterval().toMillis());
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, backOff);
        errorHandler.addNotRetryableExceptions(InvalidCustomerEventException.class, DataIntegrityViolationException.class);
        return errorHandler;
    }
}

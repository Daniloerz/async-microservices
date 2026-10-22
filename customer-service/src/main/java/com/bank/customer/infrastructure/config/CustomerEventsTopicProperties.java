package com.bank.customer.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.kafka.customer-events")
public record CustomerEventsTopicProperties(
        @NotBlank String topic,
        @Positive int partitions,
        @Positive int replicas) {
}

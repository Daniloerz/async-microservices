package com.bank.account.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

@Validated
@ConfigurationProperties(prefix = "app.kafka.customer-events")
public record CustomerEventsProperties(
        @NotBlank String topic,
        @NotBlank String deadLetterTopic,
        @Positive int partitions,
        @Positive int replicas,
        @PositiveOrZero int retryAttempts,
        @NotNull Duration retryInterval,
        @NotNull Duration retryMaxInterval) {
}

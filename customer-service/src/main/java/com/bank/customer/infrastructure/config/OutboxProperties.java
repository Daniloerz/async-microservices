package com.bank.customer.infrastructure.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.outbox")
public record OutboxProperties(
        @Positive long relayDelayMs,
        @Positive int batchSize,
        @NotNull Duration sendTimeout) {
}

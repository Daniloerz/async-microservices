package com.bank.account.infrastructure.config;

import java.time.ZoneId;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties(prefix = "app.reports")
public record ReportProperties(@NotNull ZoneId timeZone, @Positive int maxRangeDays) {
}

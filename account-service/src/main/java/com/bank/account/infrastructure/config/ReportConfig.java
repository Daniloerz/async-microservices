package com.bank.account.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.bank.account.application.report.ReportSettings;

@Configuration
@EnableConfigurationProperties(ReportProperties.class)
public class ReportConfig {

    @Bean
    ReportSettings reportSettings(ReportProperties properties) {
        return new ReportSettings(properties.timeZone(), properties.maxRangeDays());
    }
}

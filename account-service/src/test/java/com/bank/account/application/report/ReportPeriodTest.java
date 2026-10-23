package com.bank.account.application.report;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import org.junit.jupiter.api.Test;

import com.bank.account.application.exception.InvalidReportPeriodException;

class ReportPeriodTest {

    private static final ZoneId GUAYAQUIL = ZoneId.of("America/Guayaquil");
    private static final int MAX_RANGE_DAYS = 366;

    @Test
    void periodCoversWholeDaysInTheConfiguredZone() {
        ReportPeriod period = ReportPeriod.of(LocalDate.parse("2026-10-10"), LocalDate.parse("2026-10-12"),
                GUAYAQUIL, MAX_RANGE_DAYS);

        assertThat(period.fromInclusive()).isEqualTo(Instant.parse("2026-10-10T05:00:00Z"));
        assertThat(period.toExclusive()).isEqualTo(Instant.parse("2026-10-13T05:00:00Z"));
    }

    @Test
    void singleDayPeriodIsValid() {
        ReportPeriod period = ReportPeriod.of(LocalDate.parse("2026-10-23"), LocalDate.parse("2026-10-23"),
                GUAYAQUIL, MAX_RANGE_DAYS);

        assertThat(period.toExclusive()).isEqualTo(period.fromInclusive().plusSeconds(24 * 60 * 60));
    }

    @Test
    void startAfterEndIsRejected() {
        assertThatThrownBy(() -> ReportPeriod.of(LocalDate.parse("2026-10-12"), LocalDate.parse("2026-10-10"),
                GUAYAQUIL, MAX_RANGE_DAYS))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessage("must not be before startDate");
    }

    @Test
    void periodUpToTheMaximumIsAcceptedAndLongerIsRejected() {
        LocalDate start = LocalDate.parse("2026-01-01");

        assertThat(ReportPeriod.of(start, start.plusDays(MAX_RANGE_DAYS - 1), GUAYAQUIL, MAX_RANGE_DAYS)).isNotNull();
        assertThatThrownBy(() -> ReportPeriod.of(start, start.plusDays(MAX_RANGE_DAYS), GUAYAQUIL, MAX_RANGE_DAYS))
                .isInstanceOf(InvalidReportPeriodException.class)
                .hasMessage("the period must not be longer than 366 days");
    }
}

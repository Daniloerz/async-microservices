package com.bank.account.application.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bank.account.application.exception.InvalidReportPeriodException;

public record ReportPeriod(LocalDate startDate, LocalDate endDate, ZoneId zone) {

    private static final String START_DATE = "startDate";
    private static final String END_DATE = "endDate";
    private static final LocalDate EARLIEST_SUPPORTED_DATE = LocalDate.of(1900, 1, 1);
    private static final LocalDate LATEST_SUPPORTED_DATE = LocalDate.of(9999, 12, 31);
    private static final String OUT_OF_RANGE = "must be between %s and %s"
            .formatted(EARLIEST_SUPPORTED_DATE, LATEST_SUPPORTED_DATE);

    public ReportPeriod {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(zone, "zone");
    }

    public static ReportPeriod of(LocalDate startDate, LocalDate endDate, ZoneId zone, int maxRangeDays) {
        requireSupported(START_DATE, startDate);
        requireSupported(END_DATE, endDate);
        if (endDate.isBefore(startDate)) {
            throw new InvalidReportPeriodException(END_DATE, "must not be before startDate");
        }
        long days = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (days > maxRangeDays) {
            throw new InvalidReportPeriodException(END_DATE,
                    "the period must not be longer than %d days".formatted(maxRangeDays));
        }
        return new ReportPeriod(startDate, endDate, zone);
    }

    private static void requireSupported(String field, LocalDate date) {
        if (date.isBefore(EARLIEST_SUPPORTED_DATE) || date.isAfter(LATEST_SUPPORTED_DATE)) {
            throw new InvalidReportPeriodException(field, OUT_OF_RANGE);
        }
    }

    public Instant fromInclusive() {
        return startDate.atStartOfDay(zone).toInstant();
    }

    public Instant toExclusive() {
        return endDate.plusDays(1).atStartOfDay(zone).toInstant();
    }
}

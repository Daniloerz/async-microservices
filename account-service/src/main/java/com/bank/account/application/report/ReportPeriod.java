package com.bank.account.application.report;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

import com.bank.account.application.exception.InvalidReportPeriodException;

public record ReportPeriod(LocalDate startDate, LocalDate endDate, ZoneId zone) {

    private static final String END_DATE = "endDate";

    public ReportPeriod {
        Objects.requireNonNull(startDate, "startDate");
        Objects.requireNonNull(endDate, "endDate");
        Objects.requireNonNull(zone, "zone");
    }

    public static ReportPeriod of(LocalDate startDate, LocalDate endDate, ZoneId zone, int maxRangeDays) {
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

    public Instant fromInclusive() {
        return startDate.atStartOfDay(zone).toInstant();
    }

    public Instant toExclusive() {
        return endDate.plusDays(1).atStartOfDay(zone).toInstant();
    }
}

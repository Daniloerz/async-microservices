package com.bank.account.application.report;

import java.time.ZoneId;
import java.util.Objects;

public record ReportSettings(ZoneId zone, int maxRangeDays) {

    public ReportSettings {
        Objects.requireNonNull(zone, "zone");
    }
}

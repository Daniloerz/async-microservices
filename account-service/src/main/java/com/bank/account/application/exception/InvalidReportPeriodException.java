package com.bank.account.application.exception;

import lombok.Getter;

@Getter
public class InvalidReportPeriodException extends RuntimeException {

    private final String field;

    public InvalidReportPeriodException(String field, String message) {
        super(message);
        this.field = field;
    }
}

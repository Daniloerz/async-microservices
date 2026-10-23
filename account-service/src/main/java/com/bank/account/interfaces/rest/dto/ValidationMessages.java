package com.bank.account.interfaces.rest.dto;

public final class ValidationMessages {

    public static final String ACCOUNT_NUMBER_PATTERN = "^[0-9]{6,20}$";
    public static final String ACCOUNT_NUMBER = "must be 6 to 20 digits";
    public static final String MONEY = "must have at most 15 integer digits and 2 decimals";
    public static final String NOT_NEGATIVE = "must be greater than or equal to 0";
    public static final int MONEY_INTEGER_DIGITS = 15;
    public static final int MONEY_FRACTION_DIGITS = 2;

    private ValidationMessages() {
    }
}

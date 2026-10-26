package com.bank.customer.interfaces.rest.dto;

final class ValidationMessages {

    static final int MIN_AGE = 18;
    static final int MAX_AGE = 120;
    static final String IDENTIFICATION_PATTERN = "^[0-9A-Za-z-]{5,20}$";
    static final String PHONE_PATTERN = "^\\+?[0-9]{7,15}$";
    static final String NOT_BLANK_PATTERN = "(?s).*\\S.*";

    static final String MAX_100 = "must be at most 100 characters";
    static final String MAX_200 = "must be at most 200 characters";
    static final String SIZE_1_100 = "must be between 1 and 100 characters";
    static final String SIZE_1_200 = "must be between 1 and 200 characters";
    static final String IDENTIFICATION = "must be 5 to 20 letters, digits or hyphens";
    static final String PHONE = "must be 7 to 15 digits, optionally starting with +";
    static final String NOT_BLANK = "must not be blank";

    private ValidationMessages() {
    }
}

package com.bank.customer.interfaces.rest.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordLengthValidator implements ConstraintValidator<PasswordLength, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true;
        }
        int bytes = value.getBytes(StandardCharsets.UTF_8).length;
        return bytes >= PasswordLength.MIN_BYTES && bytes <= PasswordLength.MAX_BYTES;
    }
}

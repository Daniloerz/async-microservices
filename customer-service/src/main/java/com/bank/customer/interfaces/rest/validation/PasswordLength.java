package com.bank.customer.interfaces.rest.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = PasswordLengthValidator.class)
@Target({ ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT })
@Retention(RetentionPolicy.RUNTIME)
public @interface PasswordLength {

    int MIN_BYTES = 4;
    int MAX_BYTES = 72;

    String message() default "must be between " + MIN_BYTES + " and " + MAX_BYTES + " bytes";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}

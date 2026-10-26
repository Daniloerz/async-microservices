package com.bank.customer.interfaces.rest.dto;

import java.util.stream.Stream;

import com.bank.customer.domain.model.Gender;
import com.bank.customer.interfaces.rest.validation.PasswordLength;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PatchCustomerRequest(
        @Size(min = 1, max = 100, message = ValidationMessages.SIZE_1_100)
        @Pattern(regexp = ValidationMessages.NOT_BLANK_PATTERN, message = ValidationMessages.NOT_BLANK)
        String name,
        Gender gender,
        @Min(ValidationMessages.MIN_AGE) @Max(ValidationMessages.MAX_AGE) Integer age,
        @Pattern(regexp = ValidationMessages.IDENTIFICATION_PATTERN, message = ValidationMessages.IDENTIFICATION)
        String identification,
        @Size(min = 1, max = 200, message = ValidationMessages.SIZE_1_200)
        @Pattern(regexp = ValidationMessages.NOT_BLANK_PATTERN, message = ValidationMessages.NOT_BLANK)
        String address,
        @Pattern(regexp = ValidationMessages.PHONE_PATTERN, message = ValidationMessages.PHONE) String phone,
        @Schema(accessMode = Schema.AccessMode.WRITE_ONLY) @PasswordLength String password,
        Boolean active) {

    @Schema(hidden = true)
    @AssertTrue(message = "at least one field must be provided")
    public boolean isAnyFieldPresent() {
        return Stream.of(name, gender, age, identification, address, phone, password, active)
                .anyMatch(value -> value != null);
    }

    @Override
    public String toString() {
        return "PatchCustomerRequest[name=%s, gender=%s, age=%s, identification=%s, active=%s, password=%s]"
                .formatted(name, gender, age, identification, active, password == null ? null : "***");
    }
}

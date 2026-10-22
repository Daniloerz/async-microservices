package com.bank.customer.interfaces.rest.dto;

import com.bank.customer.domain.model.Gender;
import com.bank.customer.interfaces.rest.validation.PasswordLength;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateCustomerRequest(
        @Schema(example = "Jose Lema")
        @NotBlank @Size(max = 100, message = ValidationMessages.MAX_100) String name,

        @Schema(example = "MALE")
        @NotNull Gender gender,

        @Schema(example = "35")
        @NotNull @Min(ValidationMessages.MIN_AGE) @Max(ValidationMessages.MAX_AGE) Integer age,

        @Schema(example = "1712345678")
        @NotBlank @Pattern(regexp = ValidationMessages.IDENTIFICATION_PATTERN, message = ValidationMessages.IDENTIFICATION)
        String identification,

        @Schema(example = "Otavalo sn y principal")
        @NotBlank @Size(max = 200, message = ValidationMessages.MAX_200) String address,

        @Schema(example = "098254785")
        @NotBlank @Pattern(regexp = ValidationMessages.PHONE_PATTERN, message = ValidationMessages.PHONE) String phone,

        @Schema(example = "1234", accessMode = Schema.AccessMode.WRITE_ONLY)
        @NotNull @PasswordLength String password,

        @Schema(description = "Defaults to true", example = "true")
        Boolean active) {

    @Override
    public String toString() {
        return "CreateCustomerRequest[name=%s, gender=%s, age=%s, identification=%s, active=%s, password=***]"
                .formatted(name, gender, age, identification, active);
    }
}

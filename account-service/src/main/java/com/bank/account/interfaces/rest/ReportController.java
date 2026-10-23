package com.bank.account.interfaces.rest;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bank.account.application.usecase.GenerateAccountStatementUseCase;
import com.bank.account.interfaces.rest.dto.AccountStatementResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Account statements")
public class ReportController {

    private static final String PROBLEM_JSON = "application/problem+json";

    private final GenerateAccountStatementUseCase generateAccountStatementUseCase;
    private final ReportRestMapper reportRestMapper;

    @GetMapping
    @Operation(summary = "Account statement of a customer for a date range",
            description = "Both dates are included; days are cut in the configured time zone")
    @ApiResponse(responseCode = "200", description = "Account statement")
    @ApiResponse(responseCode = "400", description = "Missing or invalid parameters",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public AccountStatementResponse statement(
            @RequestParam UUID customerId,
            @Parameter(example = "2026-10-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,
            @Parameter(example = "2026-10-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate) {
        return reportRestMapper.toResponse(generateAccountStatementUseCase.generate(customerId, startDate, endDate));
    }
}

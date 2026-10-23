package com.bank.account.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.bank.account.application.usecase.CreateAccountUseCase;
import com.bank.account.application.usecase.GetAccountUseCase;
import com.bank.account.application.usecase.ListAccountsUseCase;
import com.bank.account.application.usecase.UpdateAccountUseCase;
import com.bank.account.domain.model.Account;
import com.bank.account.interfaces.rest.dto.AccountResponse;
import com.bank.account.interfaces.rest.dto.CreateAccountRequest;
import com.bank.account.interfaces.rest.dto.PageResponse;
import com.bank.account.interfaces.rest.dto.PatchAccountRequest;
import com.bank.account.interfaces.rest.dto.UpdateAccountRequest;
import com.bank.account.interfaces.rest.dto.ValidationMessages;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/accounts")
@RequiredArgsConstructor
@Tag(name = "Accounts", description = "Account management")
public class AccountController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String PROBLEM_JSON = "application/problem+json";

    private final CreateAccountUseCase createAccountUseCase;
    private final GetAccountUseCase getAccountUseCase;
    private final ListAccountsUseCase listAccountsUseCase;
    private final UpdateAccountUseCase updateAccountUseCase;
    private final AccountRestMapper accountRestMapper;

    @PostMapping
    @Operation(summary = "Open an account for an active customer")
    @ApiResponse(responseCode = "201", description = "Account created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Account number already in use",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Customer unknown or not active",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<AccountResponse> create(@Valid @RequestBody CreateAccountRequest request) {
        boolean active = !Boolean.FALSE.equals(request.active());
        Account created = createAccountUseCase.create(request.accountNumber(), request.accountType(),
                request.initialBalance(), request.customerId(), active);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{accountNumber}")
                .buildAndExpand(created.getAccountNumber())
                .toUri();
        return ResponseEntity.created(location).body(accountRestMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List accounts ordered by account number, optionally of one customer")
    @ApiResponse(responseCode = "200", description = "Page of accounts")
    @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<AccountResponse> list(
            @RequestParam(required = false) UUID customerId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return accountRestMapper.toPageResponse(listAccountsUseCase.list(customerId, page, size));
    }

    @GetMapping("/{accountNumber}")
    @Operation(summary = "Get an account")
    @ApiResponse(responseCode = "200", description = "Account found")
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public AccountResponse get(
            @PathVariable @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN,
                    message = ValidationMessages.ACCOUNT_NUMBER) String accountNumber) {
        return accountRestMapper.toResponse(getAccountUseCase.get(accountNumber));
    }

    @PutMapping("/{accountNumber}")
    @Operation(summary = "Replace the editable data of an account (type and status)")
    @ApiResponse(responseCode = "200", description = "Account updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Activation not allowed while the customer is not active",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public AccountResponse replace(
            @PathVariable @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN,
                    message = ValidationMessages.ACCOUNT_NUMBER) String accountNumber,
            @Valid @RequestBody UpdateAccountRequest request) {
        return accountRestMapper.toResponse(
                updateAccountUseCase.update(accountNumber, accountRestMapper.toChanges(request)));
    }

    @PatchMapping("/{accountNumber}")
    @Operation(summary = "Change the type or the status of an account")
    @ApiResponse(responseCode = "200", description = "Account updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Account not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Activation not allowed while the customer is not active",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public AccountResponse patch(
            @PathVariable @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN,
                    message = ValidationMessages.ACCOUNT_NUMBER) String accountNumber,
            @Valid @RequestBody PatchAccountRequest request) {
        return accountRestMapper.toResponse(
                updateAccountUseCase.update(accountNumber, accountRestMapper.toChanges(request)));
    }
}

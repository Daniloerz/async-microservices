package com.bank.account.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.bank.account.application.usecase.CorrectMovementUseCase;
import com.bank.account.application.usecase.GetMovementUseCase;
import com.bank.account.application.usecase.ListMovementsUseCase;
import com.bank.account.application.usecase.RegisterMovementUseCase;
import com.bank.account.domain.model.Movement;
import com.bank.account.interfaces.rest.dto.CreateMovementRequest;
import com.bank.account.interfaces.rest.dto.MovementResponse;
import com.bank.account.interfaces.rest.dto.PageResponse;
import com.bank.account.interfaces.rest.dto.UpdateMovementRequest;
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
@RequestMapping("/api/v1/movements")
@RequiredArgsConstructor
@Tag(name = "Movements", description = "Deposits and withdrawals on accounts")
public class MovementController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String PROBLEM_JSON = "application/problem+json";

    private final RegisterMovementUseCase registerMovementUseCase;
    private final GetMovementUseCase getMovementUseCase;
    private final ListMovementsUseCase listMovementsUseCase;
    private final CorrectMovementUseCase correctMovementUseCase;
    private final MovementRestMapper movementRestMapper;

    @PostMapping
    @Operation(summary = "Register a movement",
            description = "A positive amount is a deposit and a negative amount is a withdrawal")
    @ApiResponse(responseCode = "201", description = "Movement registered and balance updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Insufficient balance, or account unknown or not active",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<MovementResponse> register(@Valid @RequestBody CreateMovementRequest request) {
        Movement movement = registerMovementUseCase.register(request.accountNumber(), request.amount());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{movementId}")
                .buildAndExpand(movement.getMovementId())
                .toUri();
        return ResponseEntity.created(location).body(movementRestMapper.toResponse(movement));
    }

    @GetMapping
    @Operation(summary = "List movements from newest to oldest, optionally of one account")
    @ApiResponse(responseCode = "200", description = "Page of movements")
    @ApiResponse(responseCode = "400", description = "Invalid parameters",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<MovementResponse> list(
            @RequestParam(required = false) @Pattern(regexp = ValidationMessages.ACCOUNT_NUMBER_PATTERN,
                    message = ValidationMessages.ACCOUNT_NUMBER) String accountNumber,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return movementRestMapper.toPageResponse(listMovementsUseCase.list(accountNumber, page, size));
    }

    @GetMapping("/{movementId}")
    @Operation(summary = "Get a movement")
    @ApiResponse(responseCode = "200", description = "Movement found")
    @ApiResponse(responseCode = "404", description = "Movement not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public MovementResponse get(@PathVariable UUID movementId) {
        return movementRestMapper.toResponse(getMovementUseCase.get(movementId));
    }

    @PutMapping("/{movementId}")
    @Operation(summary = "Correct the amount of the last movement of its account")
    @ApiResponse(responseCode = "200", description = "Movement and account balance updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Movement not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Not the last movement of its account",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "422", description = "Insufficient balance or account not active",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public MovementResponse correct(@PathVariable UUID movementId, @Valid @RequestBody UpdateMovementRequest request) {
        return movementRestMapper.toResponse(correctMovementUseCase.correct(movementId, request.amount()));
    }
}

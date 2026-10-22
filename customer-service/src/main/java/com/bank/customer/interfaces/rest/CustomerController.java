package com.bank.customer.interfaces.rest;

import java.net.URI;
import java.util.UUID;

import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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

import com.bank.customer.application.usecase.CreateCustomerUseCase;
import com.bank.customer.application.usecase.DeleteCustomerUseCase;
import com.bank.customer.application.usecase.GetCustomerUseCase;
import com.bank.customer.application.usecase.ListCustomersUseCase;
import com.bank.customer.application.usecase.UpdateCustomerUseCase;
import com.bank.customer.domain.model.Customer;
import com.bank.customer.interfaces.rest.dto.CreateCustomerRequest;
import com.bank.customer.interfaces.rest.dto.CustomerResponse;
import com.bank.customer.interfaces.rest.dto.PageResponse;
import com.bank.customer.interfaces.rest.dto.PatchCustomerRequest;
import com.bank.customer.interfaces.rest.dto.UpdateCustomerRequest;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/v1/customers")
@RequiredArgsConstructor
@Tag(name = "Customers", description = "Customer management")
public class CustomerController {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String PROBLEM_JSON = "application/problem+json";

    private final CreateCustomerUseCase createCustomerUseCase;
    private final GetCustomerUseCase getCustomerUseCase;
    private final ListCustomersUseCase listCustomersUseCase;
    private final UpdateCustomerUseCase updateCustomerUseCase;
    private final DeleteCustomerUseCase deleteCustomerUseCase;
    private final CustomerRestMapper customerRestMapper;

    @PostMapping
    @Operation(summary = "Create a customer")
    @ApiResponse(responseCode = "201", description = "Customer created")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Identification already in use",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CreateCustomerRequest request) {
        boolean active = !Boolean.FALSE.equals(request.active());
        Customer created = createCustomerUseCase.create(customerRestMapper.toDetails(request), request.password(), active);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{customerId}")
                .buildAndExpand(created.getCustomerId())
                .toUri();
        return ResponseEntity.created(location).body(customerRestMapper.toResponse(created));
    }

    @GetMapping
    @Operation(summary = "List customers ordered by name")
    @ApiResponse(responseCode = "200", description = "Page of customers")
    @ApiResponse(responseCode = "400", description = "Invalid page or size",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public PageResponse<CustomerResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(MAX_PAGE_SIZE) int size) {
        return customerRestMapper.toPageResponse(listCustomersUseCase.list(page, size));
    }

    @GetMapping("/{customerId}")
    @Operation(summary = "Get a customer")
    @ApiResponse(responseCode = "200", description = "Customer found")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponse get(@PathVariable UUID customerId) {
        return customerRestMapper.toResponse(getCustomerUseCase.get(customerId));
    }

    @PutMapping("/{customerId}")
    @Operation(summary = "Replace the data of a customer")
    @ApiResponse(responseCode = "200", description = "Customer updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Identification already in use",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponse replace(@PathVariable UUID customerId, @Valid @RequestBody UpdateCustomerRequest request) {
        Customer updated = updateCustomerUseCase.update(customerId, customerRestMapper.toChanges(request),
                request.password());
        return customerRestMapper.toResponse(updated);
    }

    @PatchMapping("/{customerId}")
    @Operation(summary = "Change some fields of a customer")
    @ApiResponse(responseCode = "200", description = "Customer updated")
    @ApiResponse(responseCode = "400", description = "Invalid request",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    @ApiResponse(responseCode = "409", description = "Identification already in use",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public CustomerResponse patch(@PathVariable UUID customerId, @Valid @RequestBody PatchCustomerRequest request) {
        Customer updated = updateCustomerUseCase.update(customerId, customerRestMapper.toChanges(request),
                request.password());
        return customerRestMapper.toResponse(updated);
    }

    @DeleteMapping("/{customerId}")
    @Operation(summary = "Delete a customer", description = "Its accounts are deactivated asynchronously")
    @ApiResponse(responseCode = "204", description = "Customer deleted")
    @ApiResponse(responseCode = "404", description = "Customer not found",
            content = @Content(mediaType = PROBLEM_JSON, schema = @Schema(implementation = ProblemDetail.class)))
    public ResponseEntity<Void> delete(@PathVariable UUID customerId) {
        deleteCustomerUseCase.delete(customerId);
        return ResponseEntity.noContent().build();
    }
}

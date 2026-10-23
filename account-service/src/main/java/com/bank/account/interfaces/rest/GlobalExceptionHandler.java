package com.bank.account.interfaces.rest;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import com.bank.account.application.exception.AccountNotAvailableException;
import com.bank.account.application.exception.AccountNotFoundException;
import com.bank.account.application.exception.CustomerNotAvailableException;
import com.bank.account.application.exception.CustomerNotFoundException;
import com.bank.account.application.exception.DuplicateAccountNumberException;
import com.bank.account.application.exception.InvalidReportPeriodException;
import com.bank.account.application.exception.MovementNotCorrectableException;
import com.bank.account.application.exception.MovementNotFoundException;
import com.bank.account.domain.exception.AccountActivationNotAllowedException;
import com.bank.account.domain.exception.InactiveAccountException;
import com.bank.account.domain.exception.InsufficientBalanceException;
import com.bank.account.interfaces.rest.dto.FieldErrorResponse;

import lombok.extern.slf4j.Slf4j;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.exc.UnrecognizedPropertyException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final String ERRORS_PROPERTY = "errors";
    private static final String VALIDATION_FAILED = "Request validation failed";

    @ExceptionHandler(InvalidReportPeriodException.class)
    ResponseEntity<Object> handleInvalidReportPeriod(InvalidReportPeriodException exception) {
        return badRequest(VALIDATION_FAILED,
                List.of(new FieldErrorResponse(exception.getField(), exception.getMessage())));
    }

    @ExceptionHandler({ AccountNotFoundException.class, MovementNotFoundException.class,
            CustomerNotFoundException.class })
    ProblemDetail handleNotFound(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
    }

    @ExceptionHandler({ DuplicateAccountNumberException.class, MovementNotCorrectableException.class })
    ProblemDetail handleConflict(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
    }

    @ExceptionHandler({ CustomerNotAvailableException.class, AccountActivationNotAllowedException.class,
            AccountNotAvailableException.class, InactiveAccountException.class, InsufficientBalanceException.class })
    ProblemDetail handleUnprocessable(RuntimeException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.UNPROCESSABLE_CONTENT, exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnexpected(Exception exception) {
        log.error("Unexpected error", exception);
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldErrorResponse(error.getField(), messageOf(error)))
                .toList();
        return badRequest(VALIDATION_FAILED, errors);
    }

    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        List<FieldErrorResponse> errors = exception.getParameterValidationResults().stream()
                .flatMap(result -> result.getResolvableErrors().stream()
                        .map(error -> new FieldErrorResponse(result.getMethodParameter().getParameterName(),
                                messageOf(error))))
                .toList();
        return badRequest(VALIDATION_FAILED, errors);
    }

    @Override
    protected ResponseEntity<Object> handleMissingServletRequestParameter(
            MissingServletRequestParameterException exception, HttpHeaders headers, HttpStatusCode status,
            WebRequest request) {
        return badRequest("Required parameter '%s' is missing".formatted(exception.getParameterName()),
                List.of(new FieldErrorResponse(exception.getParameterName(), "is required")));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception, HttpHeaders headers,
            HttpStatusCode status, WebRequest request) {
        String parameter = exception.getPropertyName();
        return badRequest("Invalid value for parameter '%s'".formatted(parameter),
                List.of(new FieldErrorResponse(parameter, "has an invalid format")));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        Throwable cause = exception.getCause();
        if (cause instanceof UnrecognizedPropertyException unknownField) {
            return badRequest(VALIDATION_FAILED,
                    List.of(new FieldErrorResponse(unknownField.getPropertyName(), "is not a known field")));
        }
        if (cause instanceof MismatchedInputException invalidValue && !invalidValue.getPath().isEmpty()) {
            return badRequest(VALIDATION_FAILED,
                    List.of(new FieldErrorResponse(pathOf(invalidValue), "has an invalid value or type")));
        }
        return badRequest("Malformed JSON request", List.of());
    }

    private static ResponseEntity<Object> badRequest(String detail, List<FieldErrorResponse> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        if (!errors.isEmpty()) {
            problem.setProperty(ERRORS_PROPERTY, errors);
        }
        return ResponseEntity.badRequest().body(problem);
    }

    private static String messageOf(MessageSourceResolvable error) {
        return error.getDefaultMessage() == null ? "is invalid" : error.getDefaultMessage();
    }

    private static String pathOf(JacksonException exception) {
        return exception.getPath().stream()
                .map(reference -> reference.getPropertyName() != null
                        ? reference.getPropertyName()
                        : "[" + reference.getIndex() + "]")
                .collect(Collectors.joining("."));
    }
}

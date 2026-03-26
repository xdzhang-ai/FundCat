package com.winter.fund.common.exception;

import jakarta.validation.ConstraintViolationException;
import java.time.Instant;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ProblemDetail handleNotFound(NotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildProblem(HttpStatus.NOT_FOUND, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        log.warn("Validation failed: {}", exception.getMessage());
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage(),
                (left, right) -> right
            ));
        return buildProblem(HttpStatus.BAD_REQUEST, "Validation failed", Map.of("errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ProblemDetail handleConstraint(ConstraintViolationException exception) {
        log.warn("Constraint violation: {}", exception.getMessage());
        return buildProblem(HttpStatus.BAD_REQUEST, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(Exception.class)
    ProblemDetail handleUnknown(Exception exception) {
        log.error("Unhandled exception", exception);
        return buildProblem(HttpStatus.INTERNAL_SERVER_ERROR, exception.getMessage(), Map.of());
    }

    private ProblemDetail buildProblem(HttpStatus status, String detail, Map<String, Object> properties) {
        ProblemDetail problemDetail = ProblemDetail.forStatusAndDetail(status, detail);
        problemDetail.setProperty("timestamp", Instant.now());
        properties.forEach(problemDetail::setProperty);
        return problemDetail;
    }
}

package com.winter.fund.common.exception;

/**
 * 公共异常处理文件，负责统一描述和转换后端异常。
 */

import com.winter.fund.common.result.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(NotFoundException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleNotFound(NotFoundException exception) {
        log.warn("Resource not found: {}", exception.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, 404, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleBadRequest(IllegalArgumentException exception) {
        log.warn("Bad request: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 400, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleValidation(MethodArgumentNotValidException exception) {
        log.warn("Validation failed: {}", exception.getMessage());
        Map<String, String> errors = exception.getBindingResult().getFieldErrors().stream()
            .collect(java.util.stream.Collectors.toMap(
                FieldError::getField,
                fieldError -> fieldError.getDefaultMessage() == null ? "invalid" : fieldError.getDefaultMessage(),
                (left, right) -> right
            ));
        return buildResponse(HttpStatus.BAD_REQUEST, 400, "Validation failed", Map.of("errors", errors));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraint(ConstraintViolationException exception) {
        log.warn("Constraint violation: {}", exception.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, 400, exception.getMessage(), Map.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Data integrity violation: {}", exception.getMessage());
        return buildResponse(HttpStatus.CONFLICT, 409, "Request conflicts with existing data", Map.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiResponse<Map<String, Object>>> handleUnknown(Exception exception) {
        log.error("Unhandled exception", exception);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, 500, exception.getMessage(), Map.of());
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> buildResponse(
        HttpStatus status,
        int code,
        String detail,
        Map<String, Object> properties
    ) {
        return ResponseEntity.status(status).body(ApiResponse.failure(code, detail, properties));
    }
}

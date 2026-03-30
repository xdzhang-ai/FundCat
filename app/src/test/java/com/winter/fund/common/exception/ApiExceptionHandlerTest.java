package com.winter.fund.common.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.winter.fund.common.result.ApiResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ApiExceptionHandlerTest {

    private final ApiExceptionHandler handler = new ApiExceptionHandler();

    @Test
    void handleDataIntegrityMapsDuplicateKeyToConflictResponse() {
        ResponseEntity<ApiResponse<Map<String, Object>>> response =
            handler.handleDataIntegrity(new DataIntegrityViolationException("duplicate key"));

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals(409, response.getBody().code());
        assertEquals("Request conflicts with existing data", response.getBody().message());
    }
}

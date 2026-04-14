package com.example.webhookcollector.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JsonProcessingException.class)
    public ResponseEntity<Map<String, String>> handleJsonProcessing(JsonProcessingException ex) {
        log.warn("Invalid JSON in request body: {}", ex.getOriginalMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("INVALID_JSON", "Request body contains invalid JSON"));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .collect(Collectors.joining("; "));
        log.warn("Validation error: {}", message);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(errorBody("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneral(Exception ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorBody("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private Map<String, String> errorBody(String error, String message) {
        return Map.of("error", error, "message", message);
    }
}
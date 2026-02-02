package com.example.individualsapi.exception;

import com.example.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public Mono<ResponseEntity<ErrorResponse>> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("Validation error: {}", ex.getMessage());

        ErrorResponse resp = new ErrorResponse();
        resp.setError(ex.getMessage());
        resp.setStatus(HttpStatus.BAD_REQUEST.value());

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.BAD_REQUEST)
                        .body(resp)
        );
    }

    @ExceptionHandler(WebClientResponseException.class)
    public Mono<ErrorResponse> handleWebClient(WebClientResponseException ex) {
        log.error("External service error: status={}, body={}, url={}",
                ex.getStatusCode(), ex.getResponseBodyAsString(), ex.getRequest().getURI());

        ErrorResponse resp = new ErrorResponse();

        if (ex.getStatusCode().value() == 409) {
            String errorBody = ex.getResponseBodyAsString();
            if (errorBody.contains("person-service") || errorBody.contains("email already exists")) {
                resp.setError("User with this email already exists");
            } else {
                resp.setError("User already exists");
            }
            resp.setStatus(409);
            return Mono.just(resp);
        }

        if (ex.getStatusCode().value() == 400) {
            resp.setError("Invalid request data: " + ex.getResponseBodyAsString());
            resp.setStatus(400);
            return Mono.just(resp);
        }

        if (ex.getStatusCode().value() == 401) {
            resp.setError("Invalid credentials or token");
            resp.setStatus(401);
            return Mono.just(resp);
        }

        if (ex.getStatusCode().value() == 404) {
            resp.setError("Resource not found");
            resp.setStatus(404);
            return Mono.just(resp);
        }

        if (ex.getStatusCode().is5xxServerError()) {
            log.error("External service unavailable: {}", ex.getMessage());
            resp.setError("External service temporarily unavailable, please try again later");
            resp.setStatus(503);
            return Mono.just(resp);
        }

        resp.setError("External service error: " + ex.getStatusCode().value());
        resp.setStatus(ex.getStatusCode().value());
        return Mono.just(resp);
    }

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<ErrorResponse>> handleGeneric(Exception ex) {
        log.error("Unexpected error", ex);

        ErrorResponse resp = new ErrorResponse();
        resp.setError("Internal server error");
        resp.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());

        return Mono.just(
                ResponseEntity
                        .status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(resp)
        );
    }
}
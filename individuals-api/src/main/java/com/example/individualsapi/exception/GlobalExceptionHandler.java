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
        log.error("Keycloak error: status={}, body={}", ex.getStatusCode(), ex.getResponseBodyAsString());

        ErrorResponse resp = new ErrorResponse();

        if (ex.getStatusCode().value() == 409) {
            // конфликт при создании пользователя
            resp.setError("User already exists");
            resp.setStatus(409);
            return Mono.just(resp);
        }

        if (ex.getStatusCode().value() == 400 || ex.getStatusCode().value() == 401) {
            // ошибки при логине / refresh-token
            resp.setError("Invalid credentials or token");
            resp.setStatus(401);
            return Mono.just(resp);
        }

        resp.setError("Keycloak error: " + ex.getStatusCode().value());
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

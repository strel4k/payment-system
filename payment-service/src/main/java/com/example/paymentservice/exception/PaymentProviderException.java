package com.example.paymentservice.exception;

import org.springframework.http.HttpStatus;

public class PaymentProviderException extends RuntimeException {

    private final HttpStatus status;

    public PaymentProviderException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public PaymentProviderException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
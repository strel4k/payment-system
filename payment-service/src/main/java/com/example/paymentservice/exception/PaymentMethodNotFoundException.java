package com.example.paymentservice.exception;

public class PaymentMethodNotFoundException extends RuntimeException {

    public PaymentMethodNotFoundException(Integer methodId) {
        super("Payment method not found or inactive: " + methodId);
    }
}
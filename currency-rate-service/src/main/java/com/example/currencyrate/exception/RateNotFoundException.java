package com.example.currencyrate.exception;

public class RateNotFoundException extends RuntimeException {

    public RateNotFoundException(String sourceCode, String destinationCode) {
        super("Exchange rate not found for pair: " + sourceCode + " -> " + destinationCode);
    }

    public RateNotFoundException(String sourceCode, String destinationCode, String timestamp) {
        super("Exchange rate not found for pair: " + sourceCode + " -> " + destinationCode
                + " at timestamp: " + timestamp);
    }
}
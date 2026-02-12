package com.example.transaction.exception;

import java.math.BigDecimal;
import java.util.UUID;


public class InsufficientBalanceException extends RuntimeException {

    public InsufficientBalanceException(UUID walletUid, BigDecimal required, BigDecimal available) {
        super(String.format("Insufficient balance in wallet %s: required %s, available %s",
                walletUid, required, available));
    }

    public InsufficientBalanceException(String message) {
        super(message);
    }
}
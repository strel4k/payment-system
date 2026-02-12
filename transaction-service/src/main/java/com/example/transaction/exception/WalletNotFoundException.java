package com.example.transaction.exception;

import java.util.UUID;


public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID walletUid) {
        super("Wallet not found: " + walletUid);
    }

    public WalletNotFoundException(String message) {
        super(message);
    }
}
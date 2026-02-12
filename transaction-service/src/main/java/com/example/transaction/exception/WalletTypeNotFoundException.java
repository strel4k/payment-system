package com.example.transaction.exception;

import java.util.UUID;


public class WalletTypeNotFoundException extends RuntimeException {

    public WalletTypeNotFoundException(UUID walletTypeUid) {
        super("Wallet type not found: " + walletTypeUid);
    }

    public WalletTypeNotFoundException(String message) {
        super(message);
    }
}
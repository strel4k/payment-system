package com.example.transaction.exception;

import java.util.UUID;


public class DuplicateWalletException extends RuntimeException {

    public DuplicateWalletException(UUID userUid, UUID walletTypeUid) {
        super("User " + userUid + " already has wallet of type " + walletTypeUid);
    }

    public DuplicateWalletException(String message) {
        super(message);
    }
}
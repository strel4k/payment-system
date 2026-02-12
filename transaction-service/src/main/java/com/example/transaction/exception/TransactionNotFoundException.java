package com.example.transaction.exception;

import java.util.UUID;


public class TransactionNotFoundException extends RuntimeException {

  public TransactionNotFoundException(UUID transactionUid) {
    super("Transaction not found: " + transactionUid);
  }

  public TransactionNotFoundException(String message) {
    super(message);
  }
}
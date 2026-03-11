package com.example.currencyrate.exception;

public class InvalidCurrencyException extends RuntimeException {

  public InvalidCurrencyException(String code) {
    super("Unknown or inactive currency code: " + code);
  }
}
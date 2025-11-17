package com.wallet.exception;

public class InvalidTransactionStatusException extends RuntimeException {

  public InvalidTransactionStatusException(String message) {
    super(message);
  }
}

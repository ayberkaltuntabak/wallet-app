package com.wallet.exception;

public class InsufficientBalanceException extends RuntimeException {

  public InsufficientBalanceException() {
    super("Insufficient usable balance");
  }
}

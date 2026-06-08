package com.jonathan.ecommerce.exception;

public class PaymentException extends RuntimeException {
  public PaymentException(String message) {
    super(message);
  }
}

package com.jonathan.ecommerce.exception;

public class CartAccessDeniedException extends RuntimeException {
  public CartAccessDeniedException(String message) {
    super(message);
  }
}

package com.jonathan.ecommerce.exception;

public class CartConcurrencyException extends RuntimeException {
  public CartConcurrencyException(String message) {
    super(message);
  }
}

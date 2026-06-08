package com.jonathan.ecommerce.entity.enums;

public enum PaymentStatus {
  SUCCESS,
  FAILED,
  PENDING;

  public static PaymentStatus fromStripe(String stripeStatus) {
    return switch (stripeStatus) {
      case "SUCCESS" -> PaymentStatus.SUCCESS;
      case "requires_payment_method", "canceled" -> FAILED;
      default -> PENDING;
    };
  }
}

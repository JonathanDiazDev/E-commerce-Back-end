package com.jonathan.ecommerce.dto.enums;

import lombok.Getter;

@Getter
public enum MovementSortField {
  DATE("date"),
  PRICE("price"),
  QUANTITY("quantity"),
  PRODUCT_NAME("inventory.product.name");

  private final String field;

  MovementSortField(String field) {
    this.field = field;
  }

  public static MovementSortField from(String value) {
    for (MovementSortField f : values()) {
      if (f.field.equalsIgnoreCase(value)) {
        return f;
      }
    }
    return DATE;
  }
}

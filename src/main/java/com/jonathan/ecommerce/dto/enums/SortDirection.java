package com.jonathan.ecommerce.dto.enums;

import lombok.Getter;
import org.springframework.data.domain.Sort;

@Getter
public enum SortDirection {
  ASC(Sort.Direction.ASC),
  DESC(Sort.Direction.DESC);

  private final Sort.Direction direction;

  SortDirection(Sort.Direction direction) {
    this.direction = direction;
  }

  public static SortDirection from(String value) {
    if (value != null && value.equalsIgnoreCase("asc")) {
      return ASC;
    }
    return DESC;
  }
}

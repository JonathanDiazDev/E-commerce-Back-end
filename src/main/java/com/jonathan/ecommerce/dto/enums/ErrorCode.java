package com.jonathan.ecommerce.dto.enums;

import lombok.Getter;

@Getter
public enum ErrorCode {
  AUTH_INVALID_CREDENTIALS("AUTH_001"),
  AUTH_UNAUTHORIZED("AUTH_002"),
  AUTH_FORBIDDEN("AUTH_003"),

  RESOURCE_NOT_FOUND("GEN_001"),
  RESOURCE_ALREADY_EXISTS("GEN_002"),
  INSUFFICIENT_STOCK("GEN_003"),

  VALIDATION_ERROR("VAL_001"),

  INTERNAL_ERROR("SYS_001");

  private final String code;

  ErrorCode(String code) {
    this.code = code;
  }
}

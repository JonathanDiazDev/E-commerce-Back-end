package com.jonathan.ecommerce.dto.response;

public record ErrorResponse(String code, String traceId, String message, long timestamp) {}

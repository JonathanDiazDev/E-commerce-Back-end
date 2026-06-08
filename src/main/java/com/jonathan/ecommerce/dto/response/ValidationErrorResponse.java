package com.jonathan.ecommerce.dto.response;

import java.util.Map;

public record ValidationErrorResponse(Map<String, String> errors, long timestamp) {}

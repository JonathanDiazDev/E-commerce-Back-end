package com.jonathan.ecommerce.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
    Long id,
    Instant createdAt,
    String status,
    BigDecimal totalAmount,
    List<OrderItemResponse> items) {}

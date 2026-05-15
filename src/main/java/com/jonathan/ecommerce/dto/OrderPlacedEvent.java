package com.jonathan.ecommerce.dto;

import java.math.BigDecimal;
import java.util.List;

public record OrderPlacedEvent(
        String userEmail,
        String userName,
        Long orderId,
        List<String> productNames,
        BigDecimal totalAmount
) {}

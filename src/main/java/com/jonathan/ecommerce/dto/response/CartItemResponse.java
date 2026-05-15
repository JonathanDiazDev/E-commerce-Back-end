package com.jonathan.ecommerce.dto.response;

import java.math.BigDecimal;

public record CartItemResponse(
        String productName,
        Integer quantity,
        BigDecimal unitPrice,
        BigDecimal subTotal
) {}

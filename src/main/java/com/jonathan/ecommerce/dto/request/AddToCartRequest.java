package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record AddToCartRequest(
    @Positive(message = "The ID must be positive") @NotNull(message = "Product ID cannot be null")
        Long productId,
    @NotNull(message = "Quantity cannot be null")
        @Positive(message = "Quantity must be greater than 0")
        Integer quantity) {}

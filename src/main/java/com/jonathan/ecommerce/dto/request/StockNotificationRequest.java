package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockNotificationRequest(
    @NotNull(message = "El ID del producto es obligatorio")
        @Positive(message = "El ID del producto debe ser un número positivo")
        Long productId) {}

package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InventoryRequest(
    @Positive(message = "El id debe ser positivo") @NotNull(message = "Ingrese el id del producto")
        Long productId,
    @Positive(message = "La cantidad debe ser superior a 0")
        @NotNull(message = "La cantidad es requerida")
        Integer quantity) {}

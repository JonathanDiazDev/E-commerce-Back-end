package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record InventoryRequest(
    @NotNull(message = "Ingrese el id de la categoria") Long productId,
    @Min(value = 0, message = "La cantidad debe ser superior a 0") int quantity) {}

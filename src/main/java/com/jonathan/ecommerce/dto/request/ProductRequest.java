package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record ProductRequest(
    @NotBlank(message = "Ingrese el nombre del producto") String name,
    @NotBlank(message = "Ingrese la descripcion del producto") String description,
    @NotNull(message = "El precio del producto no puede ser 0") BigDecimal price,
    @NotNull(message = "Ingresa eñ id de categoria del producto") Long categoryId) {}

package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record CategoryRequest(
    @NotBlank(message = "Ingrese el nombre de la categoria") String name,
    @Positive(message = "El id de categoria padre debe ser positivo") Long parentCategoryId) {}

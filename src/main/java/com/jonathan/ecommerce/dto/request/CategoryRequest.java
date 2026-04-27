package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
    @NotBlank(message = "Ingrese el nombre de la categoria") String name, Long parentCategoryId) {}

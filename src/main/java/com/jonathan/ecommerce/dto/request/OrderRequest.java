package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotNull;

public record OrderRequest(@NotNull(message = "Cart ID is required") Long cartId) {}

package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderRequest(
    @Positive @NotNull(message = "Cart ID is required") Long cartId,
    @NotBlank(message = "Payment method ID is required") String paymentMethodId) {}

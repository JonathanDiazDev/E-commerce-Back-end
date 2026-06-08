package com.jonathan.ecommerce.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public record PaymentRequest(
    @Positive(message = "The Order-Id must be positive ") @NotNull Long orderId,
    @NotBlank(message = "Enter the name of the payment method") String paymentMethodId,
    @Positive(message = "The amount must be positive") @NotNull BigDecimal amount) {}

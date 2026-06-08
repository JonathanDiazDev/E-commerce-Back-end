package com.jonathan.ecommerce.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record RefundRequest(@Positive Long orderId, @NotBlank String reason) {}

package com.jonathan.ecommerce.dto.event;

import java.time.Instant;

public record PaymentRetryEvent(
    Long orderId,
    String paymentMethodId,
    int attemptNumber,
    String messageId,
    Instant lastAttempt) {}

package com.jonathan.ecommerce.stock.event;

import java.time.Instant;

public record FailedEmailDTO(
    String recipient, String productName, String errorMessage, Instant occurredAt) {}

package com.jonathan.ecommerce.stock.event;

import com.jonathan.ecommerce.entity.enums.EmailType;
import java.time.Instant;

public record FailedEmailDTO(
    String recipient,
    EmailType emailType,
    String payload,
    String errorMessage,
    Instant occurredAt) {}

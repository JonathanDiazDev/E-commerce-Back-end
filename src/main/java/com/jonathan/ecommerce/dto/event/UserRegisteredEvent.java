package com.jonathan.ecommerce.dto.event;

import java.time.Instant;

public record UserRegisteredEvent(
    String email, String userName, Instant createdAt, String messageId) {}

package com.jonathan.ecommerce.dto.response;

import java.time.Instant;
import java.util.UUID;

public record SessionResponse(
    UUID familyId, Instant createdAt, Instant expiresAt, String userAgent, String ipAddress) {}

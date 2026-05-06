package com.jonathan.ecommerce.dto.response;

import java.time.LocalDateTime;

public record UserResponse(
    Long id, String email, String name, String role, LocalDateTime createdAt) {}

package com.jonathan.ecommerce.dto;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String name,
        String email,
        String role
) {
}

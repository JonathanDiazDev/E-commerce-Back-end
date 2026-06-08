package com.jonathan.ecommerce.dto.event;

public record PasswordResetEvent(String email, String userName, String token, String messageId) {}

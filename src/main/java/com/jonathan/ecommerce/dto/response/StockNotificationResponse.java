package com.jonathan.ecommerce.dto.response;

import java.time.Instant;

public record StockNotificationResponse(
    String message, String productName, String userEmail, Instant createdAt) {}

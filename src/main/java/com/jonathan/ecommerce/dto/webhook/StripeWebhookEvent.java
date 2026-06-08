package com.jonathan.ecommerce.dto.webhook;

public record StripeWebhookEvent(String id, String type, StripeWebhookData data) {}

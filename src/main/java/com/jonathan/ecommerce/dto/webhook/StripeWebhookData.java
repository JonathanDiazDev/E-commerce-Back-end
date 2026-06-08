package com.jonathan.ecommerce.dto.webhook;

public record StripeWebhookData(StripePaymentIntentObject object) {}

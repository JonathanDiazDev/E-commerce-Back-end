package com.jonathan.ecommerce.dto.webhook;

public record StripePaymentIntentObject(String id, Long amount, String status) {}

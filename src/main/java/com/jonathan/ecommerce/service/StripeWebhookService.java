package com.jonathan.ecommerce.service;

import com.stripe.model.Event;

public interface StripeWebhookService {
  boolean validateWebhookSignature(String payload, String sigHeader);

  void handleEvent(Event event);

  void handlePaymentSucceeded(Event event);

  void handlePaymentFailed(Event event);

  void handlePaymentCancelled(Event event);
}

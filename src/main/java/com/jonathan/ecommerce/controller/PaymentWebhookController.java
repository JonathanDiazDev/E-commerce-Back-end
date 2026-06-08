package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.service.StripeWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class PaymentWebhookController {
  private final StripeWebhookService stripeWebhookService;

  @PostMapping("/stripe")
  public ResponseEntity<Void> handleStripeWebhook(
      @RequestBody String payload, @RequestHeader("Stripe-Signature") String signature) {
    boolean isValid = stripeWebhookService.validateWebhookSignature(payload, signature);
    if (isValid) {
      return ResponseEntity.status(HttpStatus.OK).build();
    } else {
      return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
    }
  }
}

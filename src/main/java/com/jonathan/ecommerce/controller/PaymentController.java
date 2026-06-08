package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.PaymentRequest;
import com.jonathan.ecommerce.service.PaymentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
public class PaymentController {
  private final PaymentService paymentService;

  @PostMapping("/process")
  public ResponseEntity<String> processPayment(@Valid @RequestBody PaymentRequest paymentRequest) {

    paymentService.confirmPayment(paymentRequest);

    return ResponseEntity.ok("Pago procesado con éxito");
  }
}

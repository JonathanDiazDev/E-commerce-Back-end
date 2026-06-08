package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.event.RefundRequest;
import com.jonathan.ecommerce.service.RefundService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/refunds")
@RequiredArgsConstructor
public class RefundController {
  private final RefundService refundService;

  @PostMapping
  public ResponseEntity<Void> requestRefund(@Valid @RequestBody RefundRequest request) {
    refundService.processRefund(request);
    return ResponseEntity.ok().build();
  }
}

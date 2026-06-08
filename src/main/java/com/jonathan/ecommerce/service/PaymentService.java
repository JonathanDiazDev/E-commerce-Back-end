package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.PaymentRequest;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.Payment;

public interface PaymentService {
  Payment processPayment(Order order, String paymentMethodId);

  void confirmPayment(PaymentRequest request);
}

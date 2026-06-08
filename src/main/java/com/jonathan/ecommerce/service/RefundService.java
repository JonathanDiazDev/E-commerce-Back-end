package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.event.RefundRequest;

public interface RefundService {
  void processRefund(RefundRequest request);
}

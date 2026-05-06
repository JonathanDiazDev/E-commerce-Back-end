package com.jonathan.ecommerce.service;

public interface StockNotificationService {

  void createNotification(Long productId);

  void processNotification(Long productId, Integer currentStock);
}

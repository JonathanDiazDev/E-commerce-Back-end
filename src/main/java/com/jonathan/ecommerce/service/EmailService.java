package com.jonathan.ecommerce.service;

public interface EmailService {
    void sendStockAvailabilityEmail(String to, String productName, Integer stock);
}

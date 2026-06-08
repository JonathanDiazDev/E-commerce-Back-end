package com.jonathan.ecommerce.dto.event;

public record StockEmailEvent(
    String email, String userName, String productName, Integer totalStock) {}

package com.jonathan.ecommerce.stock.event;

public record StockRestockEventDTO(
    Long productId, String productName, Integer quantityAdded, Integer totalStock) {}

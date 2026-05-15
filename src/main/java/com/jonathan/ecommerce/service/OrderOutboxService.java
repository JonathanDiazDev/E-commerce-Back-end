package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.Order;

public interface OrderOutboxService {
    void saveOutboxEvent(Order order, OrderPlacedEvent event) throws Exception;
}

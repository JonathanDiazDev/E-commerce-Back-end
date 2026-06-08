package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import java.util.List;

public interface OrderService {
  OrderResponse placeOrder(OrderRequest orderRequest);

  List<OrderResponse> getOrders();

  OrderResponse getOrderById(Long id);
}

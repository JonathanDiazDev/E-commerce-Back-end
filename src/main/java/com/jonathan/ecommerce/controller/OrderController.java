package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import com.jonathan.ecommerce.service.OrderService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;

  @PostMapping("/checkout")
  public ResponseEntity<OrderResponse> checkout(@RequestBody OrderRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(request));
  }

  @GetMapping
  public ResponseEntity<List<OrderResponse>> getAllOrders() {
    return ResponseEntity.ok().body(orderService.getOrders());
  }

  @GetMapping("/{orderid}")
  public ResponseEntity<OrderResponse> getOrderById(@PathVariable("orderid") Long orderid) {
    return ResponseEntity.ok().body(orderService.getOrderById(orderid));
  }
}

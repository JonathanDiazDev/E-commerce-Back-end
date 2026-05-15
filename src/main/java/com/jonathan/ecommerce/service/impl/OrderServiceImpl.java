package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderItemResponse;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.service.OrderOutboxService;
import com.jonathan.ecommerce.service.OrderService;
import com.jonathan.ecommerce.service.PaymentService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final SecurityHelper securityHelper;
  private final PaymentService paymentService;
  private final OrderOutboxService orderOutboxService;

  @Override
  @Transactional
  public OrderResponse placeOrder(OrderRequest request) {

    User user = securityHelper.getCurrentUser();

    Cart cart =
        cartRepository
            .findById(request.cartId())
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

    Order order = new Order();
    order.setUser(user);
    order.setCart(cart);
    order.setOrderStatus(OrderStatus.PENDING);

    List<OrderItem> orderItems =
        cart.getItems().stream()
            .map(
                cartItem -> {
                  OrderItem orderItem = new OrderItem();
                  orderItem.setProduct(cartItem.getProduct());
                  orderItem.setQuantity(cartItem.getQuantity());
                  orderItem.setPriceAtPurchase(cartItem.getUnitPrice());
                  orderItem.setOrder(order);
                  return orderItem;
                })
            .toList();

    order.setItems(orderItems);
    order.setTotalAmount(cart.getTotal());

    Order savedOrder = orderRepository.save(order);

    try {
      paymentService.processPayment(savedOrder);
      savedOrder.setOrderStatus(OrderStatus.PAID);
      OrderPlacedEvent event =
          new OrderPlacedEvent(
              order.getUser().getEmail(),
              order.getUser().getName(),
              order.getId(),
              orderItems.stream().map(item -> item.getProduct().getName()).toList(),
              order.getTotalAmount());

      orderRepository.save(savedOrder);

      orderOutboxService.saveOutboxEvent(savedOrder, event);

      cart.getItems().clear();
      cartRepository.save(cart);

    } catch (DataIntegrityViolationException e) {
      throw new DataIntegrityViolationException("Order already exists");
    } catch (Exception e) {
      throw new RuntimeException(e);
    }

    return new OrderResponse(
        savedOrder.getId(),
        savedOrder.getCreatedAt(),
        savedOrder.getOrderStatus().name(),
        order.getTotalAmount(),
        savedOrder.getItems().stream()
            .map(
                item ->
                    new OrderItemResponse(
                        item.getId(),
                        item.getProduct().getName(),
                        item.getQuantity(),
                        item.getPriceAtPurchase()))
            .toList());
  }

  @Override
  public List<OrderResponse> getOrders() {
    User user = securityHelper.getCurrentUser();
    List<Order> orders = orderRepository.findByUserId(user.getId());
    return orders.stream()
        .map(
            order ->
                new OrderResponse(
                    order.getId(),
                    order.getCreatedAt(),
                    order.getOrderStatus().name(),
                    order.getTotalAmount(),
                    order.getItems().stream()
                        .map(
                            item ->
                                new OrderItemResponse(
                                    item.getId(),
                                    item.getProduct().getName(),
                                    item.getQuantity(),
                                    item.getPriceAtPurchase()))
                        .toList()))
        .toList();
  }

  @Override
  public OrderResponse getOrderById(Long id) {
    Order order =
        orderRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    return new OrderResponse(
        order.getId(),
        order.getCreatedAt(),
        order.getOrderStatus().name(),
        order.getTotalAmount(),
        order.getItems().stream()
            .map(
                items ->
                    new OrderItemResponse(
                        items.getId(),
                        items.getProduct().getName(),
                        items.getQuantity(),
                        items.getPriceAtPurchase()))
            .toList());
  }
}

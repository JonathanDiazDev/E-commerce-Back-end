package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderItemResponse;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.InventoryNotFoundException;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.producer.PaymentRetryProducer;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.service.OrderService;
import com.jonathan.ecommerce.service.OutboxService;
import com.jonathan.ecommerce.service.PaymentService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

  private final OrderRepository orderRepository;
  private final CartRepository cartRepository;
  private final SecurityHelper securityHelper;
  private final PaymentService paymentService;
  private final OutboxService orderOutboxService;
  private final PaymentRetryProducer paymentRetryProducer;
  private final InventoryRepository inventoryRepository;

  @Override
  @Transactional
  public OrderResponse placeOrder(OrderRequest request) {
    User user = securityHelper.getCurrentUser();

    // 1. Obtener carrito y validar existencia
    Cart cart =
        cartRepository
            .findById(request.cartId())
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));

    if (cart.getItems().isEmpty()) {
      throw new IllegalStateException("Cannot place an order with an empty cart");
    }

    // 2. Bloqueo pesimista y descuento de inventario
    // Esto garantiza que nadie más pueda comprar estos productos mientras procesamos
    for (CartItem cartItem : cart.getItems()) {
      Inventory inventory =
          inventoryRepository
              .findWithLockByProductId(cartItem.getProduct().getId())
              .orElseThrow(
                  () ->
                      new InventoryNotFoundException(
                          "Stock not found for: " + cartItem.getProduct().getId()));

      if (inventory.getQuantity() < cartItem.getQuantity()) {
        throw new InsufficientStockException(
            "Not enough stock for product: " + cartItem.getProduct().getName());
      }

      inventory.setQuantity(inventory.getQuantity() - cartItem.getQuantity());
      inventoryRepository.save(inventory);
    }

    // 3. Crear Orden
    Order order = new Order();
    order.setUser(user);
    order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    order.setTotalAmount(cart.getTotal());

    cart.getItems()
        .forEach(
            cartItem -> {
              OrderItem item = new OrderItem();

              item.setProduct(cartItem.getProduct());
              item.setQuantity(cartItem.getQuantity());
              item.setPriceAtPurchase(cartItem.getUnitPrice());

              order.addOrderItem(item);
            });

    Order savedOrder = orderRepository.save(order);

    // 4. Procesar Pago y Eventos
    try {
      paymentService.processPayment(savedOrder, request.paymentMethodId());

      if (savedOrder.getPayment() != null) {
        savedOrder.setStripePaymentId(savedOrder.getPayment().getStripePaymentIntentId());
      }

      Order updateOrder =
          orderRepository
              .findById(savedOrder.getId())
              .orElseThrow(() -> new ResourceNotFoundException("Error retrieving order"));

      List<String> productNames =
          order.getItems().stream().map(item -> item.getProduct().getName()).toList();
      String messageId = UUID.randomUUID().toString();

      OrderPlacedEvent event =
          new OrderPlacedEvent(
              user.getEmail(),
              user.getName(),
              updateOrder.getId(),
              productNames,
              updateOrder.getTotalAmount(),
              messageId,
              "Por confirmar",
              "3-5 días hábiles");
      String aggregateId = order.getId().toString();
      orderOutboxService.saveOutboxEvent(
          AggregateType.ORDER, aggregateId, EventType.ORDER_PLACED, event);

      cart.getItems().clear();
      cartRepository.save(cart);

      return mapToResponse(updateOrder);

    } catch (PaymentException e) {
      log.warn("Payment failed for order {}. Triggering Kafka retry.", savedOrder.getId());
      paymentRetryProducer.sendPaymentRetryEvent(savedOrder.getId(), 1, request.paymentMethodId());
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error in placeOrder", e);
      throw new RuntimeException("Unexpected error during order processing", e);
    }
  }

  private OrderResponse mapToResponse(Order order) {
    return new OrderResponse(
        order.getId(),
        order.getCreatedAt(),
        order.getOrderStatus().name(),
        order.getTotalAmount(),
        order.getItems().stream()
            .map(
                i ->
                    new OrderItemResponse(
                        i.getId(),
                        i.getProduct().getName(),
                        i.getQuantity(),
                        i.getPriceAtPurchase()))
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

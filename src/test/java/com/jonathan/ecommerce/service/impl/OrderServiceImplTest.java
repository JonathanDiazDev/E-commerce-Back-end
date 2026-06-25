package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.request.OrderRequest;
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
import com.jonathan.ecommerce.service.OutboxService;
import com.jonathan.ecommerce.service.PaymentService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderServiceImplTest {

  @Mock private OrderRepository orderRepository;
  @Mock private CartRepository cartRepository;
  @Mock private SecurityHelper securityHelper;
  @Mock private PaymentService paymentService;
  @Mock private OutboxService orderOutboxService;
  @Mock private PaymentRetryProducer paymentRetryProducer;
  @Mock private InventoryRepository inventoryRepository;

  @InjectMocks private OrderServiceImpl orderService;

  private User user;
  private Product product;
  private Cart cart;
  private CartItem cartItem;
  private Inventory inventory;
  private OrderRequest orderRequest;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("test@test.com");
    user.setName("Test User");

    product = new Product();
    product.setId(1L);
    product.setName("Test Product");
    product.setPrice(BigDecimal.valueOf(100));

    inventory = new Inventory();
    inventory.setId(1L);
    inventory.setProduct(product);
    inventory.setQuantity(50);

    cartItem = new CartItem();
    cartItem.setProduct(product);
    cartItem.setQuantity(5);
    cartItem.setUnitPrice(BigDecimal.valueOf(100));

    cart = new Cart();
    cart.setId(1L);
    cart.setUser(user);
    cart.getItems().add(cartItem);
    cartItem.setCart(cart);

    orderRequest = new OrderRequest(1L, "pm_card_visa");
  }

  @Test
  void placeOrder_Success() throws Exception {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(inventory));

    Order savedOrder = new Order();
    savedOrder.setId(1L);
    savedOrder.setUser(user);
    savedOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    savedOrder.setTotalAmount(BigDecimal.valueOf(500));
    savedOrder.setItems(
        new ArrayList<>(
            cart.getItems().stream()
                .map(
                    ci -> {
                      OrderItem oi = new OrderItem();
                      oi.setProduct(ci.getProduct());
                      oi.setQuantity(ci.getQuantity());
                      oi.setPriceAtPurchase(ci.getUnitPrice());
                      return oi;
                    })
                .toList()));
    when(orderRepository.save(any(Order.class)))
        .thenAnswer(
            invocation -> {
              Order arg = invocation.getArgument(0);
              arg.setId(1L);
              return savedOrder;
            });

    Payment payment = new Payment();
    payment.setId(1L);
    payment.setStripePaymentIntentId("pi_123");
    savedOrder.setPayment(payment);

    when(paymentService.processPayment(any(Order.class), anyString())).thenReturn(payment);

    Order updatedOrder = new Order();
    updatedOrder.setId(1L);
    updatedOrder.setUser(user);
    updatedOrder.setOrderStatus(OrderStatus.PAID);
    updatedOrder.setTotalAmount(BigDecimal.valueOf(500));
    updatedOrder.setStripePaymentId("pi_123");
    updatedOrder.setPayment(payment);
    OrderItem orderItem = new OrderItem();
    orderItem.setId(1L);
    orderItem.setProduct(product);
    orderItem.setQuantity(5);
    orderItem.setPriceAtPurchase(BigDecimal.valueOf(100));
    updatedOrder.getItems().add(orderItem);
    when(orderRepository.findById(1L)).thenReturn(Optional.of(updatedOrder));

    doNothing().when(orderOutboxService).saveOutboxEvent(any(), anyString(), any(), any());
    when(cartRepository.save(any(Cart.class))).thenReturn(cart);

    OrderResponse result = orderService.placeOrder(orderRequest);

    assertThat(result).isNotNull();
    assertThat(result.status()).isEqualTo("PAID");
    assertThat(result.totalAmount()).isEqualTo(BigDecimal.valueOf(500));
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).productName()).isEqualTo("Test Product");

    verify(inventoryRepository).save(any(Inventory.class));
    verify(paymentService).processPayment(any(Order.class), eq("pm_card_visa"));
    verify(orderOutboxService)
        .saveOutboxEvent(
            eq(AggregateType.ORDER),
            anyString(),
            eq(EventType.ORDER_PLACED),
            any(OrderPlacedEvent.class));
    verify(cartRepository).save(any(Cart.class));
  }

  @Test
  void placeOrder_CartNotFound() {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> orderService.placeOrder(orderRequest));
    verify(orderRepository, never()).save(any());
  }

  @Test
  void placeOrder_EmptyCart() {
    cart.getItems().clear();
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

    assertThrows(IllegalStateException.class, () -> orderService.placeOrder(orderRequest));
    verify(orderRepository, never()).save(any());
  }

  @Test
  void placeOrder_InventoryNotFound() {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(InventoryNotFoundException.class, () -> orderService.placeOrder(orderRequest));
    verify(orderRepository, never()).save(any());
  }

  @Test
  void placeOrder_InsufficientStock() {
    inventory.setQuantity(2);
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(inventory));

    assertThrows(InsufficientStockException.class, () -> orderService.placeOrder(orderRequest));
    verify(orderRepository, never()).save(any());
  }

  @Test
  void placeOrder_PaymentExceptionTriggersRetry() throws Exception {
    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(inventory));

    Order savedOrder = new Order();
    savedOrder.setId(1L);
    savedOrder.setUser(user);
    savedOrder.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    savedOrder.setTotalAmount(BigDecimal.valueOf(500));
    savedOrder.setItems(
        new ArrayList<>(
            cart.getItems().stream()
                .map(
                    ci -> {
                      OrderItem oi = new OrderItem();
                      oi.setProduct(ci.getProduct());
                      oi.setQuantity(ci.getQuantity());
                      oi.setPriceAtPurchase(ci.getUnitPrice());
                      return oi;
                    })
                .toList()));
    when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

    doThrow(new PaymentException("Payment failed"))
        .when(paymentService)
        .processPayment(any(Order.class), anyString());

    assertThrows(PaymentException.class, () -> orderService.placeOrder(orderRequest));
    verify(paymentRetryProducer).sendPaymentRetryEvent(1L, 1, "pm_card_visa");
  }

  @Test
  void getOrders_Success() {
    Order order = new Order();
    order.setId(1L);
    order.setUser(user);
    order.setOrderStatus(OrderStatus.PAID);
    order.setTotalAmount(BigDecimal.valueOf(500));
    OrderItem item = new OrderItem();
    item.setId(1L);
    item.setProduct(product);
    item.setQuantity(5);
    item.setPriceAtPurchase(BigDecimal.valueOf(100));
    order.getItems().add(item);

    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(orderRepository.findByUserId(1L)).thenReturn(List.of(order));

    List<OrderResponse> result = orderService.getOrders();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).status()).isEqualTo("PAID");
  }

  @Test
  void getOrderById_Success() {
    Order order = new Order();
    order.setId(1L);
    order.setUser(user);
    order.setOrderStatus(OrderStatus.PAID);
    order.setTotalAmount(BigDecimal.valueOf(500));
    OrderItem item = new OrderItem();
    item.setId(1L);
    item.setProduct(product);
    item.setQuantity(5);
    item.setPriceAtPurchase(BigDecimal.valueOf(100));
    order.getItems().add(item);

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    OrderResponse result = orderService.getOrderById(1L);

    assertThat(result).isNotNull();
    assertThat(result.totalAmount()).isEqualTo(BigDecimal.valueOf(500));
  }

  @Test
  void getOrderById_NotFound() {
    when(orderRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(1L));
  }
}

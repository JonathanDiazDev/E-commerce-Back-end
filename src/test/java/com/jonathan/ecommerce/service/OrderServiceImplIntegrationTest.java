package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.*;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class OrderServiceImplIntegrationTest extends BaseIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private CartRepository cartRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private PaymentRepository paymentRepository;

  @MockitoBean private SecurityHelper securityHelper;
  @MockitoBean private PaymentService paymentService;

  @MockitoBean private OutboxProcessor outboxProcessor;
  @MockitoBean private ProxyManager<byte[]> proxyManager;
  @MockitoBean private StockNotificationService stockNotificationService;

  private User testUser;
  private Product testProduct;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setName("Test User");
    testUser.setEmail("order-it-" + System.nanoTime() + "@test.com");
    testUser.setPassword("encodedPass123!");
    testUser.setRole(Role.USER);
    testUser = userRepository.save(testUser);

    testProduct = new Product();
    testProduct.setName("Integration Test Product");
    testProduct.setPrice(new BigDecimal("99.99"));
    testProduct.setStatus(Status.ACTIVE);
    testProduct.setActive(true);
    testProduct = productRepository.save(testProduct);

    Inventory inventory = new Inventory();
    inventory.setProduct(testProduct);
    inventory.setQuantity(50);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    inventoryRepository.save(inventory);

    lenient().when(securityHelper.getCurrentUser()).thenReturn(testUser);
  }

  @Test
  void placeOrder_ShouldSucceed_WhenStockIsAvailable() {
    // Given
    CartItem cartItem = new CartItem();
    cartItem.setProduct(testProduct);
    cartItem.setQuantity(2);
    cartItem.setUnitPrice(testProduct.getPrice());

    Cart cart = new Cart();
    cart.setUser(testUser);
    cart.setCartStatus(CartStatus.ACTIVE);
    cart.addItem(cartItem);
    cart = cartRepository.save(cart);

    doAnswer(
            invocation -> {
              Order order = invocation.getArgument(0);
              Payment payment = new Payment();
              payment.setOrder(order);
              payment.setAmount(order.getTotalAmount());
              payment.setPaymentStatus(PaymentStatus.SUCCESS);
              payment.setStripePaymentIntentId("pi_integration_test");
              payment = paymentRepository.save(payment);

              order.setOrderStatus(OrderStatus.PAID);
              order.setPayment(payment);
              order.setStripePaymentId(payment.getStripePaymentIntentId());
              orderRepository.save(order);

              return payment;
            })
        .when(paymentService)
        .processPayment(any(Order.class), anyString());

    OrderRequest request = new OrderRequest(cart.getId(), "pm_card_visa");

    // When
    OrderResponse response = orderService.placeOrder(request);

    // Then
    assertThat(response).isNotNull();
    assertThat(response.id()).isPositive();
    assertThat(response.status()).isEqualTo("PAID");
    assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("199.98"));
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).productName()).isEqualTo("Integration Test Product");
    assertThat(response.items().get(0).quantity()).isEqualTo(2);

    Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
    assertThat(inv.getQuantity()).isEqualTo(48);

    Cart savedCart = cartRepository.findById(cart.getId()).orElseThrow();
    assertThat(savedCart.getItems()).isEmpty();

    Order savedOrder = orderRepository.findById(response.id()).orElseThrow();
    assertThat(savedOrder.getOrderStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(savedOrder.getStripePaymentId()).isEqualTo("pi_integration_test");
    assertThat(savedOrder.getPayment()).isNotNull();
    assertThat(savedOrder.getPayment().getPaymentStatus()).isEqualTo(PaymentStatus.SUCCESS);
  }

  @Test
  void placeOrder_ShouldThrowInsufficientStockException_WhenNotEnoughStock() {
    // Given
    Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
    inv.setQuantity(1);
    inventoryRepository.save(inv);

    CartItem cartItem = new CartItem();
    cartItem.setProduct(testProduct);
    cartItem.setQuantity(5);
    cartItem.setUnitPrice(testProduct.getPrice());

    Cart cart = new Cart();
    cart.setUser(testUser);
    cart.setCartStatus(CartStatus.ACTIVE);
    cart.addItem(cartItem);
    cart = cartRepository.save(cart);

    OrderRequest request = new OrderRequest(cart.getId(), "pm_card_visa");

    // When / Then
    assertThatThrownBy(() -> orderService.placeOrder(request))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Not enough stock");

    Inventory after = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
    assertThat(after.getQuantity()).isEqualTo(1);
  }

  @Test
  void placeOrder_ShouldThrowResourceNotFoundException_WhenCartDoesNotExist() {
    // Given
    OrderRequest request = new OrderRequest(9999L, "pm_card_visa");

    // When / Then
    assertThatThrownBy(() -> orderService.placeOrder(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Cart not found");
  }

  @Test
  void placeOrder_ShouldThrowIllegalStateException_WhenCartIsEmpty() {
    // Given
    Cart cart = new Cart();
    cart.setUser(testUser);
    cart.setCartStatus(CartStatus.ACTIVE);
    cart = cartRepository.save(cart);

    OrderRequest request = new OrderRequest(cart.getId(), "pm_card_visa");

    // When / Then
    assertThatThrownBy(() -> orderService.placeOrder(request))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("empty cart");
  }
}

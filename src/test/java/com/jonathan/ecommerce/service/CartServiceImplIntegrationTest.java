package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.*;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.InventoryNotFoundException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class CartServiceImplIntegrationTest extends BaseIntegrationTest {

  @Autowired private CartService cartService;
  @Autowired private CartRepository cartRepository;
  @Autowired private CartItemsRepository cartItemsRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;

  @MockitoBean private SecurityHelper securityHelper;
  @MockitoBean private OutboxProcessor outboxProcessor;
  @MockitoBean private ProxyManager<byte[]> proxyManager;
  @MockitoBean private StockNotificationService stockNotificationService;

  private User testUser;
  private Product testProduct;
  private Long inventoryId;
  private static final int INITIAL_STOCK = 20;

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setName("Cart Test User");
    testUser.setEmail("cart-it-" + System.nanoTime() + "@test.com");
    testUser.setPassword("encodedPass123!");
    testUser.setRole(Role.USER);
    testUser = userRepository.save(testUser);

    testProduct = new Product();
    testProduct.setName("Cart Test Product");
    testProduct.setPrice(new BigDecimal("49.99"));
    testProduct.setStatus(Status.ACTIVE);
    testProduct.setActive(true);
    testProduct = productRepository.save(testProduct);

    Inventory inventory = new Inventory();
    inventory.setProduct(testProduct);
    inventory.setQuantity(INITIAL_STOCK);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    inventory = inventoryRepository.save(inventory);
    inventoryId = inventory.getId();

    when(securityHelper.getCurrentUser()).thenReturn(testUser);
  }

  private Cart createCartWithItem(int quantity) {
    CartItem item = new CartItem();
    item.setProduct(testProduct);
    item.setQuantity(quantity);
    item.setUnitPrice(testProduct.getPrice());

    Cart cart = new Cart();
    cart.setUser(testUser);
    cart.setCartStatus(CartStatus.ACTIVE);
    cart.addItem(item);
    return cartRepository.save(cart);
  }

  @Test
  void addItemToCart_ShouldAddNewItem_WhenCartDoesNotExist() {
    AddToCartRequest request = new AddToCartRequest(testProduct.getId(), 3);

    CartResponse response = cartService.addItemToCart(request);

    assertThat(response).isNotNull();
    assertThat(response.id()).isPositive();
    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).productName()).isEqualTo("Cart Test Product");
    assertThat(response.items().get(0).quantity()).isEqualTo(3);
    assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("149.97"));
  }

  @Test
  void addItemToCart_ShouldIncreaseQuantity_WhenItemAlreadyInCart() {
    createCartWithItem(2);

    AddToCartRequest request = new AddToCartRequest(testProduct.getId(), 3);

    CartResponse response = cartService.addItemToCart(request);

    assertThat(response.items()).hasSize(1);
    assertThat(response.items().get(0).quantity()).isEqualTo(5);
    assertThat(response.totalAmount()).isEqualByComparingTo(new BigDecimal("249.95"));
  }

  @Test
  void addItemToCart_ShouldThrowException_WhenInsufficientStock() {
    Inventory inv = inventoryRepository.findByProductId(testProduct.getId()).orElseThrow();
    inv.setQuantity(1);
    inventoryRepository.save(inv);

    AddToCartRequest request = new AddToCartRequest(testProduct.getId(), 5);

    assertThatThrownBy(() -> cartService.addItemToCart(request))
        .isInstanceOf(InsufficientStockException.class)
        .hasMessageContaining("Not enough stock");
  }

  @Test
  void addItemToCart_ShouldThrowException_WhenProductNotFound() {
    AddToCartRequest request = new AddToCartRequest(9999L, 1);

    assertThatThrownBy(() -> cartService.addItemToCart(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("9999");
  }

  @Test
  void addItemToCart_ShouldThrowException_WhenInventoryNotFound() {
    Product noInvProduct = new Product();
    noInvProduct.setName("No Inventory Product");
    noInvProduct.setPrice(new BigDecimal("10.00"));
    noInvProduct.setStatus(Status.ACTIVE);
    noInvProduct.setActive(true);
    noInvProduct = productRepository.save(noInvProduct);

    AddToCartRequest request = new AddToCartRequest(noInvProduct.getId(), 1);

    assertThatThrownBy(() -> cartService.addItemToCart(request))
        .isInstanceOf(InventoryNotFoundException.class)
        .hasMessageContaining("No inventory");
  }

  @Test
  void getCart_ShouldReturnExistingCart() {
    Cart cart = createCartWithItem(2);

    CartResponse response = cartService.getCart();

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(cart.getId());
    assertThat(response.items()).hasSize(1);
  }

  @Test
  void getCart_ShouldCreateCart_WhenNoCartExists() {
    CartResponse response = cartService.getCart();

    assertThat(response).isNotNull();
    assertThat(response.id()).isPositive();
    assertThat(response.items()).isEmpty();
  }

  @Test
  void removeItemFromCart_ShouldRemoveAndRestoreStock() {
    Cart cart = createCartWithItem(4);
    Long itemId = cart.getItems().iterator().next().getId();

    cartService.removeItemFromCart(itemId);

    entityManager.flush();
    entityManager.clear();

    assertThat(cartItemsRepository.findById(itemId)).isEmpty();

    Inventory inv = inventoryRepository.findById(inventoryId).orElseThrow();
    assertThat(inv.getQuantity()).isEqualTo(INITIAL_STOCK + 4);
  }

  @Test
  void removeItemFromCart_ShouldReturnCart_WhenItemDoesNotExist() {
    Cart cart = createCartWithItem(2);

    CartResponse response = cartService.removeItemFromCart(9999L);

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(cart.getId());
  }

  @Test
  void clearCart_ShouldClearItemsAndRestoreStock() {
    createCartWithItem(3);

    cartService.clearCart();

    entityManager.flush();
    entityManager.clear();

    List<CartItem> remainingItems = cartItemsRepository.findAll();
    assertThat(remainingItems).isEmpty();

    Inventory inv = inventoryRepository.findById(inventoryId).orElseThrow();
    assertThat(inv.getQuantity()).isEqualTo(INITIAL_STOCK + 3);
  }
}

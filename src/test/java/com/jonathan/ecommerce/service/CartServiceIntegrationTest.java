package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.Cart;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
@EnableAutoConfiguration(exclude = {RedisAutoConfiguration.class})
public class CartServiceIntegrationTest {

  @Autowired CartService cartService;
  @Autowired CartRepository cartRepository;
  @Autowired ProductRepository productRepository;
  @Autowired InventoryRepository inventoryRepository;

  @MockitoBean SecurityHelper securityHelper;
  @MockitoBean StockNotificationService stockNotificationService;
  @MockitoBean private RedisTemplate<String, Object> redisTemplate;

  @Test
  void addItemToCart_Success() {
    User user = new User();
    user.setId(1L);
    user.setEmail("test@test.com");
    Integer quantity = 2;

    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setName("Laptop");
    product.setPrice(BigDecimal.valueOf(1500));
    product.setActive(true);
    product.setStatus(Status.ACTIVE);
    Product savedProduct = productRepository.save(product);

    Inventory inventory = new Inventory();
    inventory.setProduct(savedProduct);
    inventory.setQuantity(100);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    inventoryRepository.save(inventory);

    //    doNothing().when(stockNotificationService).createNotification(product.getId());

    AddToCartRequest request = new AddToCartRequest(savedProduct.getId(), quantity);

    CartResponse result = cartService.addItemToCart(request);

    Cart savedCart = cartRepository.findByUserId(user.getId()).get();

    assertThat(savedCart.getItems()).hasSize(1);
    assertThat(savedCart.getItems().get(0).getQuantity()).isEqualTo(quantity);

    Inventory savedInventory = inventoryRepository.findByProductId(savedProduct.getId()).get();
  }
}

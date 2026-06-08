package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.dto.mapper.CartMapper;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CartItemsRepository;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import com.jonathan.ecommerce.service.impl.CartServiceImpl;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@RequiredArgsConstructor
public class CartServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private ProductRepository productRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private CartItemsRepository cartItemsRepository;
  @Mock CartMapper cartMapper;
  @Mock private SecurityHelper securityHelper;
  @Mock private StockNotificationService stockNotificationService;

  @InjectMocks private CartServiceImpl cartService;

  @Test
  void addItemToCart_Success() {
    Long productId = 1L;
    Integer quantity = 100;
    User user = new User();
    user.setId(1L);
    user.setEmail("test@gmail.com");
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(1500));
    when(productRepository.findById(any())).thenReturn(Optional.of(product));

    when(inventoryRepository.decrementStockAtomic(productId, quantity)).thenReturn(1);

    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
    when(cartItemsRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
        .thenReturn(Optional.empty());
    CartResponse response = new CartResponse(1L, List.of(), BigDecimal.valueOf(0));
    AddToCartRequest addToCartRequest = new AddToCartRequest(productId, quantity);

    when(cartMapper.toResponse(any())).thenReturn(response);

    assertThat(cartService.addItemToCart(addToCartRequest)).isEqualTo(response);
  }

  @Test
  void addItemToCart_ProductNotFound() {
    Long productId = 1L;
    Integer quantity = 100;

    AddToCartRequest addToCartRequest = new AddToCartRequest(productId, quantity);

    when(productRepository.findById(any())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          cartService.addItemToCart(addToCartRequest);
        });
  }

  @Test
  void addItemToCart_InvalidPrice() {
    Integer quantity = 100;

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(0));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    when(productRepository.findById(any())).thenReturn(Optional.of(product));

    assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(addToCartRequest));
  }

  @Test
  void addItemToCart_InsufficientStock() {
    Integer quantity = 10;

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(100));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    Inventory inventory = new Inventory();
    inventory.setId(1L);
    inventory.setQuantity(2);

    when(inventoryRepository.decrementStockAtomic(product.getId(), quantity)).thenReturn(0);
    when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
    when(productRepository.findById(any())).thenReturn(Optional.of(product));

    assertThrows(
        InsufficientStockException.class, () -> cartService.addItemToCart(addToCartRequest));
  }

  @Test
  void addItemToCart_ManualDisabled() {
    Integer quantity = 10;

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(100));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    Inventory inventory = new Inventory();
    inventory.setId(1L);
    inventory.setQuantity(2);
    inventory.setManualDisabled(true);

    when(inventoryRepository.decrementStockAtomic(product.getId(), quantity)).thenReturn(0);
    when(inventoryRepository.findByProductId(product.getId())).thenReturn(Optional.of(inventory));
    when(productRepository.findById(any())).thenReturn(Optional.of(product));

    assertThrows(
        InsufficientStockException.class, () -> cartService.addItemToCart(addToCartRequest));
  }
}

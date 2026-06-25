package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.dto.mapper.CartMapper;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.request.UpdateQuantityRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CartItemsRepository;
import com.jonathan.ecommerce.repository.CartRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
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
class CartServiceTest {

  @Mock private CartRepository cartRepository;
  @Mock private ProductRepository productRepository;
  @Mock private InventoryRepository inventoryRepository;
  @Mock private CartItemsRepository cartItemsRepository;
  @Mock CartMapper cartMapper;
  @Mock private SecurityHelper securityHelper;

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

    when(productRepository.findById(productId)).thenReturn(Optional.of(product));

    Inventory inventory = new Inventory();
    inventory.setQuantity(200);
    inventory.setManualDisabled(false);

    when(inventoryRepository.findWithLockByProductId(productId)).thenReturn(Optional.of(inventory));

    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
    when(cartItemsRepository.findByCartIdAndProductId(cart.getId(), product.getId()))
        .thenReturn(Optional.empty());
    CartResponse response = new CartResponse(1L, List.of(), BigDecimal.valueOf(0));
    AddToCartRequest addToCartRequest = new AddToCartRequest(productId, quantity);

    when(cartMapper.toResponse(any())).thenReturn(response);

    assertThat(cartService.addItemToCart(addToCartRequest)).isEqualTo(response);
    assertThat(cart.getItems()).hasSize(1);
    assertThat(cart.getItems().getFirst().getQuantity()).isEqualTo(quantity);
    verify(cartRepository).save(cart);
  }

  @Test
  void addItemToCart_ProductNotFound() {
    User user = new User();
    user.setId(1L);

    when(securityHelper.getCurrentUser()).thenReturn(user);
    Long productId = 1L;
    Integer quantity = 2;

    AddToCartRequest addToCartRequest = new AddToCartRequest(productId, quantity);

    when(productRepository.findById(productId)).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          cartService.addItemToCart(addToCartRequest);
        });
  }

  @Test
  void addItemToCart_InvalidPrice() {
    Integer quantity = 100;

    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(0));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThrows(IllegalArgumentException.class, () -> cartService.addItemToCart(addToCartRequest));
  }

  @Test
  void addItemToCart_InsufficientStock() {
    Integer quantity = 10;
    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);
    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(100));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    Inventory inventory = new Inventory();
    inventory.setId(1L);
    inventory.setQuantity(2);
    inventory.setManualDisabled(false);

    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(inventory));
    Cart cart = new Cart();
    cart.setId(1L);

    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThrows(
        InsufficientStockException.class, () -> cartService.addItemToCart(addToCartRequest));
  }

  @Test
  void addItemToCart_ManualDisabled() {
    Integer quantity = 10;
    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);
    product.setName("test");
    product.setPrice(BigDecimal.valueOf(100));
    AddToCartRequest addToCartRequest = new AddToCartRequest(product.getId(), quantity);

    Inventory inventory = new Inventory();
    inventory.setId(1L);
    inventory.setQuantity(900);
    inventory.setManualDisabled(true);

    when(inventoryRepository.findWithLockByProductId(1L)).thenReturn(Optional.of(inventory));
    when(productRepository.findById(1L)).thenReturn(Optional.of(product));

    assertThrows(
        InsufficientStockException.class, () -> cartService.addItemToCart(addToCartRequest));
  }

  @Test
  void updateItemQuantity_SuccessAdd() {
    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);

    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

    CartItem cartItem = new CartItem();
    cartItem.setProduct(product);
    cartItem.setQuantity(5);
    cartItem.setCart(cart);
    when(cartItemsRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));

    CartResponse response = new CartResponse(1L, List.of(), BigDecimal.ZERO);

    when(cartMapper.toResponse(cart)).thenReturn(response);

    UpdateQuantityRequest request = new UpdateQuantityRequest(10);

    CartResponse result = cartService.updateItemQuantity(1L, request);

    assertThat(result).isEqualTo(response);

    assertThat(cartItem.getQuantity()).isEqualTo(10);

    verify(inventoryRepository).decrementStockAtomic(1L, 5);
  }

  @Test
  void updateItemQuantity_SuccessRestart() {
    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);

    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

    CartItem cartItem = new CartItem();
    cartItem.setProduct(product);
    cartItem.setQuantity(10);
    cartItem.setCart(cart);
    when(cartItemsRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));

    CartResponse response = new CartResponse(1L, List.of(), BigDecimal.ZERO);

    when(cartMapper.toResponse(cart)).thenReturn(response);

    UpdateQuantityRequest request = new UpdateQuantityRequest(5);

    CartResponse result = cartService.updateItemQuantity(1L, request);

    assertThat(result).isEqualTo(response);

    assertThat(cartItem.getQuantity()).isEqualTo(5);

    verify(cartItemsRepository).save(cartItem);

    verify(inventoryRepository).incrementStockAtomic(1L, 5);
  }

  @Test
  void updateItemQuantity_ShouldThrowException_WhenStockIsInsufficient() {
    User user = new User();
    user.setId(1L);
    when(securityHelper.getCurrentUser()).thenReturn(user);

    Product product = new Product();
    product.setId(1L);

    Cart cart = new Cart();
    cart.setId(1L);
    when(cartRepository.findByUserId(user.getId())).thenReturn(Optional.of(cart));

    CartItem cartItem = new CartItem();
    cartItem.setProduct(product);
    cartItem.setQuantity(10);
    cartItem.setCart(cart);
    when(cartItemsRepository.findByCartIdAndProductId(1L, 1L)).thenReturn(Optional.of(cartItem));

    UpdateQuantityRequest request = new UpdateQuantityRequest(5);

    when(inventoryRepository.incrementStockAtomic(anyLong(), anyInt()))
        .thenThrow(new InsufficientStockException("No hay stock"));

    assertThrows(
        InsufficientStockException.class,
        () -> {
          cartService.updateItemQuantity(1L, request);
        });
  }
}

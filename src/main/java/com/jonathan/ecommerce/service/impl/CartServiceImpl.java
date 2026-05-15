package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.mapper.CartMapper;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.request.UpdateQuantityRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.exception.*;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.CartService;
import com.jonathan.ecommerce.service.StockNotificationService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(rollbackFor = Exception.class)
@Slf4j
public class CartServiceImpl implements CartService {

  private final CartRepository cartRepository;
  private final ProductRepository productRepository;
  private final InventoryRepository inventoryRepository;
  private final CartItemsRepository cartItemsRepository;
  private final CartMapper cartMapper;
  private final SecurityHelper securityHelper;
  private final StockNotificationService stockNotificationService;

  @Override
  @Transactional
  public CartResponse addItemToCart(AddToCartRequest cartRequest) {
      Long productId = cartRequest.productId();
      Integer quantity = cartRequest.quantity();
    log.debug("Adding {} units of product {} to cart", quantity, productId);

    User user = securityHelper.getCurrentUser();

    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

    if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      throw new IllegalArgumentException("Product " + productId + " does not have a valid price.");
    }

    int updatedRows = inventoryRepository.decrementStockAtomic(productId, quantity);

    if (updatedRows == 0) {
      Inventory inventory =
          inventoryRepository
              .findByProductId(productId)
              .orElseThrow(
                  () ->
                      new InventoryNotFoundException(
                          "No inventory found for product: " + productId));

      try {
        stockNotificationService.createNotification(productId);
      } catch (Exception ex) {
        log.error("The stock notification could not be created for the product: {}", productId, ex);
      }

      if (inventory.isManualDisabled()) {
        throw new InsufficientStockException("Product is disabled. Cannot add to cart.");
      }

      throw new InsufficientStockException(
          "Not enough stock. Available: " + inventory.getQuantity() + ", Requested: " + quantity);
    }

    log.info("Stock decremented sucessfully for product {}", productId);

    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Cart newCart = new Cart();
                  newCart.setUser(user);
                  return cartRepository.save(newCart);
                });

    try {
      Optional<CartItem> existingItem =
          cartItemsRepository.findByCartIdAndProductId(cart.getId(), product.getId());

      if (existingItem.isPresent()) {
        CartItem item = existingItem.get();
        item.setQuantity(item.getQuantity() + quantity);
        cartItemsRepository.save(item);
        log.debug("Updated CartItem quantity for product {}", productId);
      } else {
        CartItem newItem = new CartItem();
        newItem.setQuantity(quantity);
        newItem.setProduct(product);
        newItem.setCart(cart);

        cart.addItem(newItem);
        cartItemsRepository.save(newItem);
        log.debug("Created new CartItem for product {}", productId);
      }

      log.info(
          "Item added to cart successfully. User: {}, Product: {}, Quantity: {}",
          user.getId(),
          productId,
          quantity);

      return cartMapper.toResponse(cart);

    } catch (ObjectOptimisticLockingFailureException e) {
      log.warn("Optimistic lock failure while updating cart. User: {}", user.getId());
      throw new CartConcurrencyException("Cart was modified concurrently. Please try again.");
    }
  }

  @Override
  public CartResponse updateItemQuantity(Long productId, UpdateQuantityRequest request) {
      Integer newQuantity = request.quantity();
    User user = securityHelper.getCurrentUser();
    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Cart not found"));
    CartItem item =
        cartItemsRepository
            .findByCartIdAndProductId(cart.getId(), productId)
            .orElseThrow(() -> new ResourceNotFoundException("Item not found in cart"));

    if (newQuantity <= 0) {
      removeItemFromCart(item.getId());
    }
    int currentQuantity = item.getQuantity();
    int difference = newQuantity - currentQuantity;

    if (difference > 0) {
      inventoryRepository.decrementStockAtomic(productId, difference);
    } else if (difference < 0) {
      inventoryRepository.incrementStockAtomic(productId, Math.abs(difference));
    }

    item.setQuantity(newQuantity);
    cartItemsRepository.save(item);
    return cartMapper.toResponse(item.getCart());
  }

  @Override
  public CartResponse removeItemFromCart(Long cartItemId) {
    User user = securityHelper.getCurrentUser();

    CartItem cartItem =
        cartItemsRepository
            .findById(cartItemId)
            .orElseThrow(() -> new CartItemNotFoundException("CartItem not found: " + cartItemId));

    if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
      throw new UnauthorizedException("Cannot remove item from another user's cart");
    }

    inventoryRepository.decrementStockAtomic(cartItem.getProduct().getId(), cartItem.getQuantity());

    cartItemsRepository.delete(cartItem);
    log.info("Item removed from cart. Quantity: {} returned to inventory", cartItem.getQuantity());

    return cartMapper.toResponse(cartItem.getCart());
  }

  @Override
  public CartResponse getCart() {
    User user = securityHelper.getCurrentUser();

    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Cart not found for user: " + user.getId()));

    return cartMapper.toResponse(cart);
  }

  @Override
  public void clearCart() {
    User user = securityHelper.getCurrentUser();

    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseThrow(
                () -> new ResourceNotFoundException("Cart not found for user: " + user.getId()));

    for (CartItem item : cart.getItems()) {
      inventoryRepository.incrementStockAtomic(item.getProduct().getId(), item.getQuantity());
    }

    cartItemsRepository.deleteAll(cart.getItems());
    log.info("Cart cleared for user: {}", user.getId());
  }

}

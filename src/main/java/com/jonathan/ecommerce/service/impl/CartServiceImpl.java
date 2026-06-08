package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.mapper.CartMapper;
import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.request.UpdateQuantityRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.CartStatus;
import com.jonathan.ecommerce.exception.*;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.CartService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  @Override
  @Transactional
  public CartResponse addItemToCart(AddToCartRequest cartRequest) {

    Long productId = cartRequest.productId();
    Integer quantity = cartRequest.quantity();

    log.debug(
        "Iniciando proceso de añadir al carrito. Producto: {}, Cantidad: {}", productId, quantity);

    User user = securityHelper.getCurrentUser();

    // 1. Validar producto
    Product product =
        productRepository
            .findById(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + productId));

    if (product.getPrice() == null || product.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
      log.error("Error de integridad: El producto {} tiene precio inválido.", productId);
      throw new IllegalArgumentException("Product " + productId + " does not have a valid price.");
    }

    // 2. Bloqueo pesimista de inventario
    Inventory inventory =
        inventoryRepository
            .findWithLockByProductId(productId)
            .orElseThrow(
                () ->
                    new InventoryNotFoundException("No inventory found for product: " + productId));

    if (inventory.isManualDisabled()) {
      log.warn("Intento de añadir producto deshabilitado: {}", productId);
      throw new InsufficientStockException("Product is currently disabled.");
    }

    // 3. Obtener o crear carrito
    Cart cart = getOrCreateCart(user);

    try {

      // 4. Buscar item existente
      Optional<CartItem> existingItem =
          cartItemsRepository.findByCartIdAndProductId(cart.getId(), product.getId());

      if (existingItem.isPresent()) {

        CartItem item = existingItem.get();

        int newQuantity = item.getQuantity() + quantity;

        // Validar stock TOTAL
        if (inventory.getQuantity() < newQuantity) {

          log.warn(
              "Stock insuficiente para producto {}. Requerido: {}, Disponible: {}",
              productId,
              newQuantity,
              inventory.getQuantity());

          throw new InsufficientStockException(
              "Not enough stock. Available: " + inventory.getQuantity());
        }

        item.setQuantity(newQuantity);

        // Actualizar precio vigente
        item.setUnitPrice(product.getPrice());

        log.debug(
            "Cantidad actualizada para el producto {} en el carrito del usuario {}",
            productId,
            user.getId());

      } else {

        // Validar stock inicial
        if (inventory.getQuantity() < quantity) {

          log.warn(
              "Stock insuficiente para producto {}. Requerido: {}, Disponible: {}",
              productId,
              quantity,
              inventory.getQuantity());

          throw new InsufficientStockException(
              "Not enough stock. Available: " + inventory.getQuantity());
        }

        CartItem newItem = new CartItem();

        newItem.setQuantity(quantity);
        newItem.setProduct(product);
        newItem.setUnitPrice(product.getPrice());

        // Mantiene ambos lados sincronizados
        cart.addItem(newItem);

        log.debug(
            "Nuevo item creado para el producto {} en el carrito del usuario {}",
            productId,
            user.getId());
      }

      // Hibernate dirty checking + cascade
      cartRepository.save(cart);

      log.info("Producto {} añadido con éxito al carrito del usuario {}", productId, user.getId());

      return cartMapper.toResponse(cart);

    } catch (Exception e) {

      log.error(
          "Error crítico al actualizar el carrito para el usuario {}: {}",
          user.getId(),
          e.getMessage(),
          e);

      throw e;
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

    // 1. Intentar buscar el ítem
    Optional<CartItem> cartItemOpt = cartItemsRepository.findById(cartItemId);

    if (cartItemOpt.isEmpty()) {
      log.warn("Intento de borrar un item que ya no existe: {}", cartItemId);
      // Si no existe, devolvemos el estado actual del carrito del usuario
      Cart cart =
          cartRepository
              .findByUserId(user.getId())
              .orElseThrow(() -> new ResourceNotFoundException("Cart not found for user"));
      return cartMapper.toResponse(cart);
    }

    CartItem cartItem = cartItemOpt.get();

    // 2. Validar que el ítem pertenezca al usuario logueado
    if (!cartItem.getCart().getUser().getId().equals(user.getId())) {
      throw new CartAccessDeniedException("Cannot remove item from another user's cart");
    }

    // 3. Devolver stock al inventario (OJO: Aquí deberías incrementar, no decrementar)
    // Asumo que tu método en el repositorio hace un incremento real (ej: quantity + increment)
    inventoryRepository.incrementStockAtomic(cartItem.getProduct().getId(), cartItem.getQuantity());

    // 4. Eliminar el ítem
    cartItemsRepository.delete(cartItem);
    log.info("Item removed from cart. Quantity: {} returned to inventory", cartItem.getQuantity());

    // 5. Retornar carrito actualizado
    return cartMapper.toResponse(cartItem.getCart());
  }

  @Override
  public CartResponse getCart() {
    User user = securityHelper.getCurrentUser();

    Cart cart =
        cartRepository
            .findByUserId(user.getId())
            .orElseGet(
                () -> {
                  Cart newCart = new Cart();
                  newCart.setUser(user);
                  newCart.setCartStatus(CartStatus.ACTIVE);
                  return cartRepository.save(newCart);
                });

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

  private Cart getOrCreateCart(User user) {

    return cartRepository
        .findByUserId(user.getId())
        .orElseGet(
            () -> {
              Cart newCart = new Cart();
              newCart.setUser(user);
              newCart.setCartStatus(CartStatus.ACTIVE);

              log.info("Creado nuevo carrito para el usuario: {}", user.getId());
              return cartRepository.save(newCart);
            });
  }
}

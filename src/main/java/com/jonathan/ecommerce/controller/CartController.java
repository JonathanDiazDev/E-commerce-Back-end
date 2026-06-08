package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.request.UpdateQuantityRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/carts")
@RequiredArgsConstructor
public class CartController {
  private final CartService cartService;

  @PostMapping("/item")
  public ResponseEntity<CartResponse> addItem(@Valid @RequestBody AddToCartRequest cartRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(cartService.addItemToCart(cartRequest));
  }

  @DeleteMapping("/item/{cartItemId}")
  public ResponseEntity<CartResponse> removeItemFromCart(@PathVariable Long cartItemId) {
    return ResponseEntity.ok(cartService.removeItemFromCart(cartItemId));
  }

  @PutMapping("/item/{cartItemId}")
  public ResponseEntity<CartResponse> updateItemQuantity(
      @PathVariable Long cartItemId, @Valid @RequestBody UpdateQuantityRequest request) {
    return ResponseEntity.ok(cartService.updateItemQuantity(cartItemId, request));
  }

  @GetMapping
  public ResponseEntity<CartResponse> getCart() {
    return ResponseEntity.ok(cartService.getCart());
  }
}

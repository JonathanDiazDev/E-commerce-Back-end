package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.AddToCartRequest;
import com.jonathan.ecommerce.dto.request.UpdateQuantityRequest;
import com.jonathan.ecommerce.dto.response.CartResponse;

import java.math.BigDecimal;

public interface CartService {
    CartResponse addItemToCart(AddToCartRequest cartRequest);
    CartResponse removeItemFromCart(Long cartItemId);
    CartResponse updateItemQuantity(Long productId, UpdateQuantityRequest request);
    CartResponse getCart();
    void clearCart();
}

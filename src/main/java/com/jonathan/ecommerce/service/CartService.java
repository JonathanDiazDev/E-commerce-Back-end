package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.response.CartResponse;

public interface CartService {
    CartResponse addItemToCart(Long productId, Integer quantity);
    CartResponse removeItemFromCart(Long cartItemId);
}

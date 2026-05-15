package com.jonathan.ecommerce.dto.mapper;

import com.jonathan.ecommerce.dto.response.CartItemResponse;
import com.jonathan.ecommerce.dto.response.CartResponse;
import com.jonathan.ecommerce.entity.Cart;
import com.jonathan.ecommerce.entity.CartItem;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface CartMapper {

    @Mapping(source = "total", target = "totalAmount")
    CartResponse toResponse(Cart cart);

    @Mapping(source = "product.name", target = "productName") // Corregido: productName
    @Mapping(source = "product.price", target = "unitPrice")   // Corregido: fuente del precio
    @Mapping(target = "subTotal",
            expression = "java(item.getProduct().getPrice().multiply(new java.math.BigDecimal(item.getQuantity())))") // Corregido: paréntesis
    CartItemResponse toItemResponse(CartItem item);
}

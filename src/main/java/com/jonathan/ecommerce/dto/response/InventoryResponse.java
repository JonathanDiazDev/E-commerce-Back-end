package com.jonathan.ecommerce.dto.response;

import com.jonathan.ecommerce.entity.enums.InventoryStatus;

public record InventoryResponse(
    Long productId, Integer quantity, InventoryStatus inventoryStatus) {}

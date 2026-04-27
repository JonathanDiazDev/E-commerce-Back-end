package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import java.util.List;

public interface InventoryService {

  InventoryResponse getStockDetails(Long productId);

  List<InventoryResponse> getMovementHistory(Long productId);

  InventoryResponse updateInventoryStatus(Long productId, InventoryStatus inventoryStatus);

  InventoryResponse addStock(Long productId, Integer quantity, String reason);

  InventoryResponse deductStock(Long productId, Integer quantity, String reason);
}

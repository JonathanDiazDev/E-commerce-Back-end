package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.MovementFilterRequest;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import org.springframework.data.domain.Page;

public interface InventoryService {

  InventoryResponse getStockDetails(Long productId);

  Page<MovementResponse> getMovementHistory(Long productId, MovementFilterRequest request);

  InventoryResponse updateInventoryStatus(Long productId, InventoryStatus inventoryStatus);

  InventoryResponse addStock(Long productId, Integer quantity, String reason);

  InventoryResponse deductStock(Long productId, Integer quantity, String reason);
}

package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.enums.MovementSortField;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface InventoryService {

  InventoryResponse getStockDetails(Long productId);

  Page<MovementResponse> getMovementHistory(
      Long productId,
      int page,
      int size,
      MovementSortField sortBy,
      Sort.Direction direction,
      MovementType type);

  InventoryResponse updateInventoryStatus(Long productId, InventoryStatus inventoryStatus);

  InventoryResponse addStock(Long productId, Integer quantity, String reason);

  InventoryResponse deductStock(Long productId, Integer quantity, String reason);
}

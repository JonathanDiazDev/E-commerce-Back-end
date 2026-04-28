package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.enums.MovementSortField;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.enums.MovementType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public interface MovementService {

  void recordMovement(
      Inventory inventory, Integer quantity, MovementType movementType, String reason);

  Page<MovementResponse> getHistoryByProduct(
      Long productId,
      int page,
      int size,
      MovementSortField sortBy,
      Sort.Direction direction,
      MovementType type);
}

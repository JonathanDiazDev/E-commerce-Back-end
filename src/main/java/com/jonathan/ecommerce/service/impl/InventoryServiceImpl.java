package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.service.InventoryService;
import com.jonathan.ecommerce.service.MovementService;
import jakarta.transaction.Transactional;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

  private final InventoryRepository inventoryRepository;
  private final MovementService movementService;

  private InventoryResponse toResponse(Inventory inventory) {
    return new InventoryResponse(
        inventory.getId(), inventory.getQuantity(), inventory.getInventoryStatus());
  }

  @Override
  public InventoryResponse getStockDetails(Long productId) {
    return toResponse(
        inventoryRepository
            .findById(productId)
            .orElseThrow(
                () ->
                    new ResourceNotFoundException("Product with id " + productId + " not found")));
  }

  @Override
  public List<InventoryResponse> getMovementHistory(Long productId) {
    return null;
  }

  @Override
  public InventoryResponse updateInventoryStatus(Long productId, InventoryStatus inventoryStatus) {
    return null;
  }

  @Override
  public InventoryResponse addStock(Long productId, Integer quantity, String reason) {
    return null;
  }

  @Override
  @Transactional
  public InventoryResponse deductStock(Long productId, Integer quantity, String reason) {

    Inventory inventory =
        inventoryRepository
            .findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));

    if (inventory.getQuantity() < quantity || inventory.isManualDisabled()) {
      throw new InsufficientStockException(
          "Product with id "
              + productId
              + " have not enough stock, stock is"
              + inventory.getQuantity());
    }

    inventory.setQuantity(inventory.getQuantity() - quantity);
    inventoryRepository.save(inventory);

    movementService.recordMovement(inventory, quantity, MovementType.OUT, reason);
    return toResponse(inventory);
  }
}

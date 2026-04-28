package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.enums.MovementSortField;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.service.InventoryService;
import com.jonathan.ecommerce.service.MovementService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
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
  public Page<MovementResponse> getMovementHistory(
      Long productId,
      int page,
      int size,
      MovementSortField sortBy,
      Sort.Direction direction,
      MovementType type) {

    inventoryRepository
        .findByProductId(productId)
        .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    return movementService.getHistoryByProduct(productId, page, size, sortBy, direction, type);
  }

  @Override
  @Transactional
  public InventoryResponse updateInventoryStatus(Long productId, InventoryStatus inventoryStatus) {
    Inventory inventory =
        inventoryRepository
            .findByProductId(productId)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product with id " + productId + " not found"));
    inventory.setInventoryStatus(inventoryStatus);
    inventoryRepository.save(inventory);
    return toResponse(inventory);
  }

  @Override
  @Transactional
  public InventoryResponse addStock(Long productId, Integer quantity, String reason) {
    Inventory inventory =
        inventoryRepository
            .findByProductId(productId)
            .orElseThrow(() -> new ResourceNotFoundException("Inventory not found"));
    inventory.setQuantity(inventory.getQuantity() + quantity);
    inventoryRepository.save(inventory);

    movementService.recordMovement(inventory, quantity, MovementType.IN, reason);
    return toResponse(inventory);
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

package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.MovementFilterRequest;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.InventoryMovement;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.MovementRepository;
import com.jonathan.ecommerce.service.MovementService;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.apachecommons.CommonsLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@CommonsLog
@Service
@RequiredArgsConstructor
public class MovementServiceImpl implements MovementService {

  private final MovementRepository movementRepository;
  private final InventoryRepository inventoryRepository;

  private static final DateTimeFormatter FORMATTER =
      DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm").withZone(ZoneId.systemDefault());

  private MovementResponse toResponse(InventoryMovement movement) {

    String dateAsString =
        Optional.ofNullable(movement.getDate())
            .map(FORMATTER::format)
            .orElse("Fecha no disponible");

    Instant dateAsInstant = Optional.ofNullable(movement.getDate()).orElse(Instant.EPOCH);

    String productName =
        Optional.ofNullable(movement.getInventory())
            .map(Inventory::getProduct)
            .map(Product::getName)
            .orElse("Unidentified product");

    return new MovementResponse(
        movement.getId(),
        productName,
        movement.getMovementType(),
        movement.getQuantity(),
        movement.getReason(),
        dateAsInstant,
        dateAsString);
  }

  @Override
  @Transactional
  public void recordMovement(
      Inventory inventory, Integer quantity, MovementType movementType, String reason) {
    if (inventory == null) {
      throw new IllegalArgumentException("The inventory cannot be null to record a movement");
    }

    if (quantity == 0) {
      throw new IllegalArgumentException("The quantity cannot be zero");
    }

    if (!inventoryRepository.existsById(inventory.getId())) {
      throw new ResourceNotFoundException("Inventory not found");
    }

    int finalQuantity =
        switch (movementType) {
          case SALE -> Math.abs(quantity) * -1;
          case RESTOCK -> Math.abs(quantity);
          case RETURN -> Math.abs(quantity);
          case ADJUSTMENT -> quantity;
        };

    InventoryMovement inventoryMovement = new InventoryMovement();
    inventoryMovement.setInventory(inventory);
    inventoryMovement.setMovementType(movementType);
    inventoryMovement.setQuantity(finalQuantity);
    inventoryMovement.setReason(reason);
    movementRepository.save(inventoryMovement);
  }

  @Override
  public Page<MovementResponse> getHistoryByProduct(Long productId, MovementFilterRequest request) {

    Sort sort = Sort.by(Sort.Direction.fromString(request.direction()), request.sortBy());

    Pageable pageable = PageRequest.of(request.page(), request.size(), sort);

    Page<InventoryMovement> movementPage;

    if (request.type() != null) {
      movementPage =
          movementRepository.findByInventoryIdAndType(productId, request.type(), pageable);
    } else {
      movementPage = movementRepository.findByInventoryId(productId, pageable);
    }

    return movementPage.map(this::toResponse);
  }
}

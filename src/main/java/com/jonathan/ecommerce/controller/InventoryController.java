package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.enums.MovementSortField;
import com.jonathan.ecommerce.dto.enums.SortDirection;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v4/inventory")
@RequiredArgsConstructor
public class InventoryController {
  private final InventoryService inventoryService;

  @GetMapping("/{productId}")
  public ResponseEntity<InventoryResponse> getDetails(@PathVariable Long productId) {
    return ResponseEntity.ok(inventoryService.getStockDetails(productId));
  }

  @GetMapping("/product/{productId}/history")
  public ResponseEntity<Page<MovementResponse>> getProductHistory(
      @PathVariable Long productId,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "10") int size,
      @RequestParam(defaultValue = "date") String sortBy,
      @RequestParam(defaultValue = "desc") String direction,
      @RequestParam(required = false) MovementType type) {
    MovementSortField sortField = MovementSortField.from(sortBy);
    Sort.Direction sortDir = SortDirection.from(direction).getDirection();
    if (sortField == MovementSortField.DATE && !sortBy.equalsIgnoreCase("date")) {
      log.warn("Campo inválido: {}. Reemplazado por DATE.", sortBy);
    }
    Page<MovementResponse> history =
        inventoryService.getMovementHistory(productId, page, size, sortField, sortDir, type);
    return ResponseEntity.ok(history);
  }

  @PostMapping("/{productId}/add")
  public ResponseEntity<InventoryResponse> addStock(
      @PathVariable Long productId, @RequestParam Integer quantity, @RequestParam String reason) {
    return ResponseEntity.ok(inventoryService.addStock(productId, quantity, reason));
  }

  @PostMapping("/{productId}/deduct")
    public ResponseEntity<InventoryResponse> deductStock(@PathVariable Long productId, @RequestParam Integer quantity, @RequestParam String reason) {
      return ResponseEntity.ok(inventoryService.deductStock(productId, quantity, reason));
  }

  @PatchMapping("/{productId}/status")
    public ResponseEntity<InventoryResponse> updateStatus(
            @PathVariable Long productId, @RequestParam(required = true) InventoryStatus status
  ){
      return ResponseEntity.ok(inventoryService.updateInventoryStatus(productId, status));
  }
}

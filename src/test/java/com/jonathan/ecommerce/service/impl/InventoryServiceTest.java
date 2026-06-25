package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

  @Mock private InventoryRepository inventoryRepository;

  @Mock private MovementServiceImpl movementService;

  @InjectMocks private InventoryServiceImpl inventoryService;

  @Test
  void getStockDetails_Success() {
    Long productId = 1L;
    Long inventoryId = 2L;
    Inventory inventory = new Inventory();
    inventory.setId(inventoryId);
    inventory.setQuantity(10);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    InventoryResponse expected = new InventoryResponse(inventoryId, 10, InventoryStatus.IN_STOCK);
    when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

    assertThat(inventoryService.getStockDetails(productId)).isEqualTo(expected);
  }

  @Test
  void getStockDetails_NotFound() {
    Long productId = 1L;
    when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          inventoryService.getStockDetails(productId);
        });
  }

  @Test
  void testDeductStock_Success() {
    Long productId = 1L;
    Long inventoryId = 1L;
    Integer quantity = 100;
    String reason = "reason";

    Category category = new Category();
    category.setId(1L);
    category.setName("Electrónicos");

    Product product = new Product();
    product.setId(productId);
    product.setName("laptop");
    product.setDescription("Gaming Laptop");
    product.setPrice(BigDecimal.valueOf(15000));
    product.setCategory(category);
    product.setActive(true);
    product.setStatus(Status.ACTIVE);

    Inventory inventory = new Inventory();
    inventory.setId(inventoryId);
    inventory.setProduct(product);
    inventory.setQuantity(quantity);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);

    InventoryResponse expected =
        new InventoryResponse(inventoryId, 0, InventoryStatus.OUT_OF_STOCK);

    when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));
    doNothing()
        .when(movementService)
        .recordMovement(any(Inventory.class), anyInt(), any(MovementType.class), anyString());

    InventoryResponse result = inventoryService.deductStock(productId, quantity, reason);
    assertThat(result).isEqualTo(expected);

    verify(inventoryRepository).save(inventory);
    verify(movementService).recordMovement(inventory, quantity, MovementType.SALE, reason);
  }

  @Test
  void testDedustStock_InsufficientStock() {
    Long productId = 1L;
    Integer quantity = 100;
    String reason = "reason";
    Long inventoryId = 1L;

    Category category = new Category();
    category.setId(1L);
    category.setName("Electrónicos");

    Inventory inventory = new Inventory();
    inventory.setId(inventoryId);
    inventory.setQuantity(0);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);

    when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

    assertThrows(
        InsufficientStockException.class,
        () -> {
          inventoryService.deductStock(productId, quantity, reason);
        });
  }

  @Test
  void testDeductStock_ManualDisabled() {
    Long productId = 1L;
    Integer quantity = 100;
    String reason = "reason";
    Long inventoryId = 1L;

    Inventory inventory = new Inventory();
    inventory.setId(inventoryId);
    inventory.setQuantity(500);
    inventory.setManualDisabled(true);

    when(inventoryRepository.findByProductId(productId)).thenReturn(Optional.of(inventory));

    assertThrows(
        InsufficientStockException.class,
        () -> {
          inventoryService.deductStock(productId, quantity, reason);
        });
  }
}

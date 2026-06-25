package com.jonathan.ecommerce.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.MovementFilterRequest;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.InventoryMovement;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.MovementRepository;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class MovementServiceImplTest {

  @Mock private MovementRepository movementRepository;

  @Mock private InventoryRepository inventoryRepository;

  @InjectMocks private MovementServiceImpl movementService;

  private Inventory inventory;
  private Product product;

  @BeforeEach
  void setUp() {
    product = new Product();
    product.setId(1L);
    product.setName("Test Product");

    inventory = new Inventory();
    inventory.setId(1L);
    inventory.setProduct(product);
    inventory.setQuantity(100);
  }

  @Test
  void recordMovement_shouldRecordSaleMovementSuccessfully() {
    when(inventoryRepository.existsById(anyLong())).thenReturn(true);

    movementService.recordMovement(inventory, 10, MovementType.SALE, "Sale reason");

    verify(movementRepository, times(1)).save(any(InventoryMovement.class));
    verify(movementRepository, times(1))
        .save(
            argThat(
                movement ->
                    movement.getInventory().equals(inventory)
                        && movement.getMovementType().equals(MovementType.SALE)
                        && movement.getQuantity() == -10
                        && movement.getReason().equals("Sale reason")));
  }

  @Test
  void recordMovement_shouldRecordRestockMovementSuccessfully() {
    when(inventoryRepository.existsById(anyLong())).thenReturn(true);

    movementService.recordMovement(inventory, 20, MovementType.RESTOCK, "Restock reason");

    verify(movementRepository, times(1)).save(any(InventoryMovement.class));
    verify(movementRepository, times(1))
        .save(
            argThat(
                movement ->
                    movement.getInventory().equals(inventory)
                        && movement.getMovementType().equals(MovementType.RESTOCK)
                        && movement.getQuantity() == 20
                        && movement.getReason().equals("Restock reason")));
  }

  @Test
  void recordMovement_shouldRecordReturnMovementSuccessfully() {
    when(inventoryRepository.existsById(anyLong())).thenReturn(true);

    movementService.recordMovement(inventory, 5, MovementType.RETURN, "Return reason");

    verify(movementRepository, times(1)).save(any(InventoryMovement.class));
    verify(movementRepository, times(1))
        .save(
            argThat(
                movement ->
                    movement.getInventory().equals(inventory)
                        && movement.getMovementType().equals(MovementType.RETURN)
                        && movement.getQuantity() == 5
                        && movement.getReason().equals("Return reason")));
  }

  @Test
  void recordMovement_shouldRecordAdjustmentMovementSuccessfully() {
    when(inventoryRepository.existsById(anyLong())).thenReturn(true);

    movementService.recordMovement(inventory, -15, MovementType.ADJUSTMENT, "Adjustment reason");

    verify(movementRepository, times(1)).save(any(InventoryMovement.class));
    verify(movementRepository, times(1))
        .save(
            argThat(
                movement ->
                    movement.getInventory().equals(inventory)
                        && movement.getMovementType().equals(MovementType.ADJUSTMENT)
                        && movement.getQuantity() == -15
                        && movement.getReason().equals("Adjustment reason")));
  }

  @Test
  void recordMovement_shouldThrowExceptionWhenInventoryIsNull() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> movementService.recordMovement(null, 10, MovementType.SALE, "Reason"));

    assertEquals("The inventory cannot be null to record a movement", exception.getMessage());
    verifyNoInteractions(movementRepository);
    verifyNoInteractions(inventoryRepository);
  }

  @Test
  void recordMovement_shouldThrowExceptionWhenQuantityIsZero() {
    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () -> movementService.recordMovement(inventory, 0, MovementType.SALE, "Reason"));

    assertEquals("The quantity cannot be zero", exception.getMessage());
    verifyNoInteractions(movementRepository);
    verifyNoInteractions(inventoryRepository);
  }

  @Test
  void recordMovement_shouldThrowResourceNotFoundExceptionWhenInventoryDoesNotExist() {
    when(inventoryRepository.existsById(anyLong())).thenReturn(false);

    ResourceNotFoundException exception =
        assertThrows(
            ResourceNotFoundException.class,
            () -> movementService.recordMovement(inventory, 10, MovementType.SALE, "Reason"));

    assertEquals("Inventory not found", exception.getMessage());
    verify(inventoryRepository, times(1)).existsById(inventory.getId());
    verifyNoInteractions(movementRepository);
  }

  @Test
  void getHistoryByProduct_shouldReturnMovementHistoryWithoutTypeFilter() {
    MovementFilterRequest request = new MovementFilterRequest(0, 10, "date", "DESC", null);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"));

    InventoryMovement movement1 = new InventoryMovement();
    movement1.setId(1L);
    movement1.setInventory(inventory);
    movement1.setMovementType(MovementType.SALE);
    movement1.setQuantity(-10);
    movement1.setReason("Sale");
    movement1.setDate(Instant.now());

    InventoryMovement movement2 = new InventoryMovement();
    movement2.setId(2L);
    movement2.setInventory(inventory);
    movement2.setMovementType(MovementType.RESTOCK);
    movement2.setQuantity(20);
    movement2.setReason("Restock");
    movement2.setDate(Instant.now().minusSeconds(3600));

    Page<InventoryMovement> movementPage =
        new PageImpl<>(Arrays.asList(movement1, movement2), pageable, 2);

    when(movementRepository.findByInventoryId(anyLong(), any(Pageable.class)))
        .thenReturn(movementPage);

    Page<MovementResponse> result = movementService.getHistoryByProduct(1L, request);

    assertNotNull(result);
    assertEquals(2, result.getTotalElements());
    assertEquals("Test Product", result.getContent().get(0).productName());
    assertEquals(MovementType.SALE, result.getContent().get(0).movementType());
    assertEquals(-10, result.getContent().get(0).quantity());
    assertEquals("Test Product", result.getContent().get(1).productName());
    assertEquals(MovementType.RESTOCK, result.getContent().get(1).movementType());
    assertEquals(20, result.getContent().get(1).quantity());

    verify(movementRepository, times(1)).findByInventoryId(1L, pageable);
    verify(movementRepository, never())
        .findByInventoryIdAndType(anyLong(), any(MovementType.class), any(Pageable.class));
  }

  @Test
  void getHistoryByProduct_shouldReturnMovementHistoryWithTypeFilter() {
    MovementFilterRequest request =
        new MovementFilterRequest(0, 10, "date", "ASC", MovementType.SALE);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "date"));

    InventoryMovement movement1 = new InventoryMovement();
    movement1.setId(1L);
    movement1.setInventory(inventory);
    movement1.setMovementType(MovementType.SALE);
    movement1.setQuantity(-10);
    movement1.setReason("Sale");
    movement1.setDate(Instant.now());

    Page<InventoryMovement> movementPage =
        new PageImpl<>(Collections.singletonList(movement1), pageable, 1);

    when(movementRepository.findByInventoryIdAndType(
            anyLong(), any(MovementType.class), any(Pageable.class)))
        .thenReturn(movementPage);

    Page<MovementResponse> result = movementService.getHistoryByProduct(1L, request);

    assertNotNull(result);
    assertEquals(1, result.getTotalElements());
    assertEquals("Test Product", result.getContent().get(0).productName());
    assertEquals(MovementType.SALE, result.getContent().get(0).movementType());
    assertEquals(-10, result.getContent().get(0).quantity());

    verify(movementRepository, times(1)).findByInventoryIdAndType(1L, MovementType.SALE, pageable);
    verify(movementRepository, never()).findByInventoryId(anyLong(), any(Pageable.class));
  }

  @Test
  void getHistoryByProduct_shouldReturnEmptyPageWhenNoMovementsFound() {
    MovementFilterRequest request = new MovementFilterRequest(0, 10, "date", "DESC", null);
    Pageable pageable = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "date"));

    Page<InventoryMovement> emptyPage = new PageImpl<>(Collections.emptyList(), pageable, 0);

    when(movementRepository.findByInventoryId(anyLong(), any(Pageable.class)))
        .thenReturn(emptyPage);

    Page<MovementResponse> result = movementService.getHistoryByProduct(1L, request);

    assertNotNull(result);
    assertTrue(result.isEmpty());
    assertEquals(0, result.getTotalElements());

    verify(movementRepository, times(1)).findByInventoryId(1L, pageable);
  }
}

package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.MovementFilterRequest;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.exception.InsufficientStockException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.service.MovementService;
import com.jonathan.ecommerce.stock.event.StockRestockEventDTO;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

@ExtendWith(MockitoExtension.class)
class InventoryServiceImplTest {

  @Mock private InventoryRepository inventoryRepository;
  @Mock private MovementService movementService;
  @Mock private ApplicationEventPublisher eventPublisher;

  @InjectMocks private InventoryServiceImpl inventoryService;

  @Captor private ArgumentCaptor<StockRestockEventDTO> eventCaptor;
  @Captor private ArgumentCaptor<Inventory> inventoryCaptor;

  private Product product;
  private Inventory inventory;

  @BeforeEach
  void setUp() {
    product = new Product();
    product.setId(1L);
    product.setName("Test Product");

    inventory = new Inventory();
    inventory.setId(1L);
    inventory.setProduct(product);
    inventory.setQuantity(100);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    inventory.setManualDisabled(false);
  }

  @Test
  void getStockDetails_Success() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    InventoryResponse result = inventoryService.getStockDetails(1L);

    assertThat(result.quantity()).isEqualTo(100);
    assertThat(result.inventoryStatus()).isEqualTo(InventoryStatus.IN_STOCK);
  }

  @Test
  void getStockDetails_NotFound() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> inventoryService.getStockDetails(1L));
  }

  @Test
  void addStock_Success() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InventoryResponse result = inventoryService.addStock(1L, 50, "Restock from supplier");

    // 1. Validar respuesta del servicio
    assertThat(result.quantity()).isEqualTo(150);

    // 2. Validar que la entidad mutó antes de persistirse
    verify(inventoryRepository).save(inventoryCaptor.capture());
    assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(150);

    // 3. Validar payload del evento disparado
    verify(eventPublisher).publishEvent(eventCaptor.capture());
    StockRestockEventDTO publishedEvent = eventCaptor.getValue();
    assertThat(publishedEvent.productId()).isEqualTo(1L);
    assertThat(publishedEvent.totalStock()).isEqualTo(150);

    verify(movementService)
        .recordMovement(inventory, 50, MovementType.RESTOCK, "Restock from supplier");
  }

  @Test
  void addStock_NotFound() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> inventoryService.addStock(1L, 10, "test"));
    verify(inventoryRepository, never()).save(any());
  }

  @Test
  void deductStock_Success() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InventoryResponse result = inventoryService.deductStock(1L, 30, "Order #123");

    assertThat(result.quantity()).isEqualTo(70);

    verify(inventoryRepository).save(inventoryCaptor.capture());
    assertThat(inventoryCaptor.getValue().getQuantity()).isEqualTo(70);

    verify(movementService).recordMovement(inventory, 30, MovementType.SALE, "Order #123");
  }

  @Test
  void deductStock_NotFound() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> inventoryService.deductStock(1L, 10, "test"));
    verify(inventoryRepository, never()).save(any());
  }

  @Test
  void deductStock_InsufficientStock() {
    inventory.setQuantity(5);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    assertThrows(
        InsufficientStockException.class, () -> inventoryService.deductStock(1L, 10, "test"));
    verify(inventoryRepository, never()).save(any());
  }

  @Test
  void deductStock_ManualDisabled() {
    inventory.setManualDisabled(true);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));

    assertThrows(
        InsufficientStockException.class, () -> inventoryService.deductStock(1L, 10, "test"));
    verify(inventoryRepository, never()).save(any());
  }

  @Test
  void deductStock_SetsOutOfStockWhenZero() {
    inventory.setQuantity(10);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InventoryResponse result = inventoryService.deductStock(1L, 10, "Last item");

    assertThat(result.quantity()).isEqualTo(0);
    assertThat(result.inventoryStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);

    verify(inventoryRepository).save(inventoryCaptor.capture());
    assertThat(inventoryCaptor.getValue().getInventoryStatus())
        .isEqualTo(InventoryStatus.OUT_OF_STOCK);
  }

  @Test
  void updateInventoryStatus_Success() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(inventoryRepository.save(any(Inventory.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    InventoryResponse result =
        inventoryService.updateInventoryStatus(1L, InventoryStatus.OUT_OF_STOCK);

    assertThat(result.inventoryStatus()).isEqualTo(InventoryStatus.OUT_OF_STOCK);

    verify(inventoryRepository).save(inventoryCaptor.capture());
    assertThat(inventoryCaptor.getValue().getInventoryStatus())
        .isEqualTo(InventoryStatus.OUT_OF_STOCK);
  }

  @Test
  void updateInventoryStatus_NotFound() {
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> inventoryService.updateInventoryStatus(1L, InventoryStatus.OUT_OF_STOCK));
  }

  @Test
  void getMovementHistory_Success() {
    MovementFilterRequest request = new MovementFilterRequest(0, 10, "date", "DESC", null);
    MovementResponse movement =
        new MovementResponse(
            1L, "Test Product", MovementType.SALE, -10, "Sale", Instant.now(), "2024-01-01");

    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.of(inventory));
    when(movementService.getHistoryByProduct(eq(1L), any(MovementFilterRequest.class)))
        .thenReturn(new PageImpl<>(List.of(movement)));

    Page<MovementResponse> result = inventoryService.getMovementHistory(1L, request);

    assertThat(result).isNotEmpty();
    assertThat(result.getContent().get(0).productName()).isEqualTo("Test Product");
  }

  @Test
  void getMovementHistory_InventoryNotFound() {
    MovementFilterRequest request = new MovementFilterRequest(0, 10, "date", "DESC", null);
    when(inventoryRepository.findByProductId(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class, () -> inventoryService.getMovementHistory(1L, request));
  }
}

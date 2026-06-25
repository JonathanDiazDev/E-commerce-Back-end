package com.jonathan.ecommerce.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.service.impl.MovementServiceImpl;
import com.jonathan.ecommerce.stock.event.StockRestockEventDTO;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
class InventoryServiceIntegrationTest extends BaseIntegrationTest {

  @Autowired private InventoryService inventoryService;

  @Autowired private InventoryRepository inventoryRepository;

  @Autowired private ProductRepository productRepository;

  @Autowired private ApplicationEvents applicationEvents;

  @MockitoSpyBean private MovementServiceImpl movementService;

  @MockitoBean private EmailService emailService;

  @MockitoBean private OutboxProcessor outboxProcessor;

  @MockitoBean private ProxyManager<byte[]> proxyManager;

  @MockitoBean private StockNotificationService stockNotificationService;

  private Long productId;

  @BeforeEach
  void setup() {
    Product product = new Product();
    product.setName("laptop test");
    product.setPrice(new BigDecimal(1500));
    product.setStatus(Status.ACTIVE);
    product = productRepository.save(product);
    productId = product.getId();

    Inventory inventory = new Inventory();
    inventory.setProduct(product);
    inventory.setQuantity(10);
    inventory.setInventoryStatus(InventoryStatus.IN_STOCK);
    inventory.setManualDisabled(false);

    inventoryRepository.save(inventory);
  }

  @Test
  void addStock_ShouldIncreaseQuantityAndPublishEvent_WhenProductExists() {
    Integer cantidadAAgregar = 5;
    String razon = "Restock de mercancía";

    InventoryResponse response = inventoryService.addStock(productId, cantidadAAgregar, razon);

    assertEquals(15, response.quantity());

    Inventory inventoryDB =
        inventoryRepository
            .findByProductId(productId)
            .orElseThrow(() -> new AssertionError("El inventario no se encontró en la BD"));

    assertEquals(15, inventoryDB.getQuantity());

    long matches = applicationEvents.stream(StockRestockEventDTO.class).count();
    assertEquals(1, matches);

    StockRestockEventDTO eventDTO =
        applicationEvents.stream(StockRestockEventDTO.class)
            .findFirst()
            .orElseThrow(() -> new AssertionError("El evento no fue publicado"));

    assertEquals(5, eventDTO.quantityAdded());
    assertEquals("laptop test", eventDTO.productName());
    assertEquals(15, eventDTO.totalStock());
  }

  @Test
  void deductStock_ShouldDecreaseQuantityAndPublishEvent_WhenProductExistsAndHasEnoughStock() {
    Integer cantidadARestar = 3;
    String razon = "Compra de cliente";

    InventoryResponse response = inventoryService.deductStock(productId, cantidadARestar, razon);
    assertEquals(7, response.quantity());

    Inventory inventoryDB =
        inventoryRepository
            .findByProductId(productId)
            .orElseThrow(() -> new AssertionError("El inventario no se encontro en la BD"));

    assertEquals(7, inventoryDB.getQuantity());
  }
}

package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.*;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ProductServiceImplIntegrationTest extends BaseIntegrationTest {

  @Autowired private ProductService productService;
  @Autowired private ProductRepository productRepository;
  @Autowired private CategoryRepository categoryRepository;
  @Autowired private InventoryRepository inventoryRepository;

  @MockitoBean private SecurityHelper securityHelper;
  @MockitoBean private OutboxProcessor outboxProcessor;
  @MockitoBean private ProxyManager<byte[]> proxyManager;
  @MockitoBean private StockNotificationService stockNotificationService;

  private Category testCategory;

  @BeforeEach
  void setUp() {
    testCategory = new Category();
    testCategory.setName("Test Category");
    testCategory.setActive(true);
    testCategory = categoryRepository.save(testCategory);
  }

  private Product insertProduct(String name, BigDecimal price) {
    Product p = new Product();
    p.setName(name);
    p.setPrice(price);
    p.setStatus(Status.ACTIVE);
    p.setActive(true);
    p.setCategory(testCategory);
    return productRepository.save(p);
  }

  @Test
  void addProduct_ShouldCreateProductAndInventory_WhenCategoryExists() {
    ProductRequest request =
        new ProductRequest(
            "New Product", "Description", new BigDecimal("29.99"), 10, testCategory.getId());

    ProductResponse response = productService.addProduct(request);

    assertThat(response).isNotNull();
    assertThat(response.id()).isPositive();
    assertThat(response.name()).isEqualTo("new product");
    assertThat(response.description()).isEqualTo("Description");
    assertThat(response.price()).isEqualByComparingTo(new BigDecimal("29.99"));
    assertThat(response.categoryName()).isEqualTo("Test Category");
    assertThat(response.status()).isEqualTo("ACTIVE");

    Inventory inventory = inventoryRepository.findByProductId(response.id()).orElse(null);
    assertThat(inventory).isNotNull();
    assertThat(inventory.getQuantity()).isEqualTo(10);
  }

  @Test
  void addProduct_ShouldThrowException_WhenDuplicateName() {
    insertProduct("existing product", new BigDecimal("10.00"));

    ProductRequest request =
        new ProductRequest(
            "Existing Product", "Description", new BigDecimal("29.99"), 5, testCategory.getId());

    assertThatThrownBy(() -> productService.addProduct(request))
        .isInstanceOf(ResourceAlreadyExistsException.class)
        .hasMessageContaining("already exists");
  }

  @Test
  void getProductById_ShouldReturnProduct_WhenExists() {
    Product product = insertProduct("get-by-id-test", new BigDecimal("9.99"));

    ProductResponse response = productService.getProductById(product.getId());

    assertThat(response).isNotNull();
    assertThat(response.id()).isEqualTo(product.getId());
    assertThat(response.name()).isEqualTo("get-by-id-test");
  }

  @Test
  void getProductById_ShouldThrowException_WhenNotFound() {
    assertThatThrownBy(() -> productService.getProductById(9999L))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("9999");
  }

  @Test
  void getAllProducts_ShouldReturnOnlyActiveProducts() {
    Product active1 = insertProduct("active-one", new BigDecimal("1.00"));
    Product active2 = insertProduct("active-two", new BigDecimal("2.00"));
    Product inactive = insertProduct("inactive", new BigDecimal("3.00"));
    inactive.setActive(false);
    productRepository.save(inactive);

    List<ProductResponse> products = productService.getAllProducts();

    assertThat(products).hasSize(2);
    assertThat(products).extracting(ProductResponse::name).contains("active-one", "active-two");
  }

  @Test
  void updateProduct_ShouldUpdateFields() {
    Product product = insertProduct("original-name", new BigDecimal("10.00"));
    ProductRequest updateRequest =
        new ProductRequest(
            "Updated Name", "Updated desc", new BigDecimal("20.00"), 0, testCategory.getId());

    ProductResponse response = productService.updateProduct(product.getId(), updateRequest);

    assertThat(response.name()).isEqualTo("updated name");
    assertThat(response.description()).isEqualTo("Updated desc");
    assertThat(response.price()).isEqualByComparingTo(new BigDecimal("20.00"));
    assertThat(response.categoryName()).isEqualTo("Test Category");
  }

  @Test
  void deactivateProduct_ShouldSetInactive() {
    Product product = insertProduct("to-deactivate", new BigDecimal("5.00"));

    productService.deactivateProduct(product.getId());

    Product updated = productRepository.findById(product.getId()).orElseThrow();
    assertThat(updated.getActive()).isFalse();
  }

  @Test
  void getProductsByCategory_ShouldReturnMatchingProducts() {
    Product p1 = new Product();
    p1.setName("cat-product-a");
    p1.setPrice(new BigDecimal("11.00"));
    p1.setStatus(Status.ACTIVE);
    p1.setActive(true);
    p1.setCategory(testCategory);
    productRepository.save(p1);

    Product p2 = new Product();
    p2.setName("cat-product-b");
    p2.setPrice(new BigDecimal("22.00"));
    p2.setStatus(Status.ACTIVE);
    p2.setActive(true);
    p2.setCategory(testCategory);
    productRepository.save(p2);

    List<ProductResponse> products = productService.getProductsByCategory("Test Category");

    assertThat(products).hasSize(2);
    assertThat(products)
        .extracting(ProductResponse::name)
        .contains("cat-product-a", "cat-product-b");
  }

  @Test
  void getProductByName_ShouldReturnProducts() {
    insertProduct("find-me", new BigDecimal("7.00"));

    List<ProductResponse> products = productService.getProductByName("find-me");

    assertThat(products).hasSize(1);
    assertThat(products.get(0).name()).isEqualTo("find-me");
  }

  @Test
  void getProductsByPrice_ShouldReturnProductsInRange() {
    insertProduct("cheap", new BigDecimal("5.00"));
    insertProduct("medium", new BigDecimal("15.00"));
    insertProduct("expensive", new BigDecimal("50.00"));

    List<ProductResponse> products =
        productService.getProductsByPrice(new BigDecimal("10.00"), new BigDecimal("30.00"));

    assertThat(products).hasSize(1);
    assertThat(products.get(0).name()).isEqualTo("medium");
  }

  @Test
  void getProductsByPrice_ShouldReturnEmpty_WhenNoMatch() {
    insertProduct("cheap", new BigDecimal("5.00"));

    List<ProductResponse> products =
        productService.getProductsByPrice(new BigDecimal("0.01"), new BigDecimal("0.99"));

    assertThat(products).isEmpty();
  }
}

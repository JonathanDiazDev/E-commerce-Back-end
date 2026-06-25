package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Inventory;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.repository.InventoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

  @Mock private ProductRepository productRepository;

  @Mock private CategoryRepository categoryRepository;

  @Mock private InventoryRepository inventoryRepository;

  @InjectMocks private ProductServiceImpl productService;

  @Test
  void testAddProduct_Success() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Electrónicos");

    Product product = new Product();
    ProductRequest request =
        new ProductRequest("laptop", "Gaming Laptop", BigDecimal.valueOf(15000), 1, 1L);

    product.setCategory(category);
    product.setPrice(BigDecimal.valueOf(15000));
    product.setActive(true);
    product.setName(request.name());
    product.setDescription(request.description());
    product.setStatus(Status.ACTIVE);

    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    when(productRepository.save(any(Product.class))).thenReturn(product);

    when(inventoryRepository.save(any(Inventory.class))).thenAnswer(inv -> inv.getArgument(0));

    ProductResponse result = productService.addProduct(request);

    assertThat(result.name().toLowerCase()).isEqualTo("laptop");
    assertThat(result.description()).isEqualTo("Gaming Laptop");
    assertThat(result.price()).isEqualTo(BigDecimal.valueOf(15000));
    assertThat(result.categoryName()).isEqualTo("Electrónicos");
  }

  @Test
  void testAddProduct_AlreadyExists() {
    ProductRequest request =
        new ProductRequest("Laptop", "Gaming Laptop", BigDecimal.valueOf(15000), 1, 1L);

    when(productRepository.existsByName("laptop")).thenReturn(true);

    assertThrows(
        ResourceAlreadyExistsException.class,
        () -> {
          productService.addProduct(request);
        });

    verify(productRepository, never()).save(any(Product.class));
  }

  @Test
  void testAddProduct_CategoryNotFound() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Electronica");

    ProductRequest request =
        new ProductRequest("Laptop", "Gaming Laptop", BigDecimal.valueOf(15000), 1, 1L);

    when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          productService.addProduct(request);
        });
  }

  @Test
  void testUpdateProduct_Success() {
    Category category = new Category();
    category.setId(1L);
    category.setName("Electrónicos");

    Long id = 1L;
    Product product = new Product();
    ProductRequest request =
        new ProductRequest("laptop", "Gaming Laptop", BigDecimal.valueOf(15000), 1, 1L);

    product.setId(1L);
    product.setCategory(category);
    product.setPrice(BigDecimal.valueOf(15000));
    product.setActive(true);
    product.setName(request.name());
    product.setDescription(request.description());
    product.setStatus(Status.ACTIVE);
    ProductResponse expected =
        new ProductResponse(
            id, "laptop", "Gaming Laptop", BigDecimal.valueOf(15000), "Electrónicos", "ACTIVE");

    when(productRepository.findById(id)).thenReturn(Optional.of(product));
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(productRepository.save(any(Product.class))).thenReturn(product);

    ProductResponse result = productService.updateProduct(id, request);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testUpdateProduct_ProductNotFound() {

    Long id = 1L;
    ProductRequest request =
        new ProductRequest("laptop", "Gaming Laptop", BigDecimal.valueOf(15000), 1, 1L);

    when(productRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          productService.updateProduct(id, request);
        });
    verify(productRepository).findById(id);
  }

  @Test
  void testGetProductById_Success() {
    Long id = 1L;
    Category category = new Category();
    category.setId(1L);
    category.setName("Electrónicos");
    Product product = new Product();
    product.setId(id);
    product.setName("laptop");
    product.setDescription("Gaming Laptop");
    product.setPrice(BigDecimal.valueOf(15000));
    product.setCategory(category);
    product.setActive(true);
    product.setStatus(Status.ACTIVE);

    ProductResponse expected =
        new ProductResponse(
            id, "laptop", "Gaming Laptop", BigDecimal.valueOf(15000), "Electrónicos", "ACTIVE");

    when(productRepository.findById(id)).thenReturn(Optional.of(product));
    ProductResponse result = productService.getProductById(id);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void testGetProductById_ProductNotFound() {
    Long id = 1L;
    when(productRepository.findById(id)).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> {
          productService.getProductById(id);
        });
  }
}

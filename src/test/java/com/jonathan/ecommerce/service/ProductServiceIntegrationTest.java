package com.jonathan.ecommerce.service;

import static org.junit.jupiter.api.Assertions.*;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public class ProductServiceIntegrationTest extends BaseIntegrationTest {
  @Autowired ProductService productService;

  @Autowired ProductRepository productRepository;

  @Autowired CategoryRepository categoryRepository;

  @Test
  void shouldCreateProduct() {

    Category category = new Category();
    category.setName("electronica");
    category = categoryRepository.save(category);

    ProductRequest request =
        new ProductRequest(
            "LAPTOP GAMER", "HIGH PERFORMANCE", new BigDecimal(1500), 1, category.getId());

    ProductResponse response = productService.addProduct(request);

    assertNotNull(response.id());
    assertEquals("laptop gamer", response.name());
    assertEquals("electronica", response.categoryName());

    assertTrue(productRepository.existsById(response.id()));
  }
}

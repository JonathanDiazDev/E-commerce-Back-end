package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.entity.Product;
import com.jonathan.ecommerce.entity.enums.Status;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.repository.ProductRepository;
import com.jonathan.ecommerce.service.ProductService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;

  private ProductResponse toResponse(Product product) {
    return new ProductResponse(
        product.getId(),
        product.getName(),
        product.getDescription(),
        product.getPrice(),
        product.getCategory().getName(),
        product.getStatus().toString());
  }

  @Override
  public ProductResponse addProduct(ProductRequest request) {
    if (productRepository.findByName(request.name()).isPresent()) {
      throw new ResourceAlreadyExistsException(
          "Product with name " + request.name() + " already exists");
    }
    Product product = new Product();
    product.setName(request.name());
    product.setDescription(request.description());
    product.setPrice(request.price());
    if (request.categoryId() != null) {
      Category category =
          categoryRepository
              .findById(request.categoryId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Category with id " + request.categoryId() + " not found"));
      product.setCategory(category);
    }
    product.setActive(true);
    product.setStatus(Status.ACTIVE);
    productRepository.save(product);
    return toResponse(product);
  }

  @Override
  public ProductResponse updateProduct(Long id, ProductRequest request) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product with id " + id + " not found"));
    product.setName(request.name());
    product.setDescription(request.description());
    product.setPrice(request.price());
    if (request.categoryId() != null) {
      Category category =
          categoryRepository
              .findById(request.categoryId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          "Category with id " + request.categoryId() + " not found"));
      product.setCategory(category);
    }
    product.setActive(true);
    productRepository.save(product);
    return toResponse(product);
  }

  @Override
  public List<ProductResponse> getAllProducts() {
    return productRepository.findByActiveTrue().stream().map(this::toResponse).toList();
  }

  @Override
  public ProductResponse getProductById(Long id) {
    return toResponse(
        productRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product with id " + id + " not found")));
  }

  @Override
  public List<ProductResponse> getProductsByCategory(String category) {
    return productRepository
        .findByCategory(
            categoryRepository
                .findByName(category)
                .orElseThrow(
                    () ->
                        new ResourceNotFoundException(
                            "Category with name " + category + " not found")))
        .stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public ProductResponse getProductsByName(String name) {
    return toResponse(
        productRepository
            .findByName(name)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product with name " + name + " not found")));
  }

  @Override
  public List<ProductResponse> getProductsByPrice(BigDecimal min, BigDecimal max) {
    return productRepository.findByPriceBetween(min, max).stream().map(this::toResponse).toList();
  }

  @Override
  public void deactivateProduct(Long id) {
    Product product =
        productRepository
            .findById(id)
            .orElseThrow(
                () -> new ResourceNotFoundException("Product with id " + id + " not found"));
    product.setActive(false);
    productRepository.save(product);
  }
}

package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.ProductRequest;
import com.jonathan.ecommerce.dto.response.ProductResponse;
import com.jonathan.ecommerce.service.ProductService;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v3/product")
@RequiredArgsConstructor
public class ProductController {

  private final ProductService productService;

  @PostMapping("/create")
  public ResponseEntity<ProductResponse> addProduct(@Valid @RequestBody ProductRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(productService.addProduct(request));
  }

  @PutMapping("/{id}")
  public ResponseEntity<ProductResponse> updateProduct(
      @PathVariable Long id, @Valid @RequestBody ProductRequest request) {
    return ResponseEntity.status(HttpStatus.OK).body(productService.updateProduct(id, request));
  }

  @GetMapping
  public ResponseEntity<List<ProductResponse>> findAll() {
    return ResponseEntity.status(HttpStatus.OK).body(productService.getAllProducts());
  }

  @GetMapping("/{id}")
  public ResponseEntity<ProductResponse> getProductById(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.OK).body(productService.getProductById(id));
  }

  @GetMapping("/category")
  public ResponseEntity<List<ProductResponse>> getProductsByCategory(
      @RequestParam String category) {
    return ResponseEntity.status(HttpStatus.OK)
        .body(productService.getProductsByCategory(category));
  }

  @GetMapping("/name")
  public ResponseEntity<ProductResponse> getProductsByName(@RequestParam String name) {
    return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByName(name));
  }

  @GetMapping("/price")
  public ResponseEntity<List<ProductResponse>> getProductsByPrice(
      @RequestParam BigDecimal min, @RequestParam BigDecimal max) {
    return ResponseEntity.status(HttpStatus.OK).body(productService.getProductsByPrice(min, max));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivateProduct(@PathVariable Long id) {
    productService.deactivateProduct(id);
    return ResponseEntity.noContent().build();
  }
}

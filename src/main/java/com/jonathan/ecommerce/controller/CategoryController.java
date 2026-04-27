package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;
import com.jonathan.ecommerce.service.CategoryService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2/categories")
@RequiredArgsConstructor
public class CategoryController {
  private final CategoryService categoryService;

  @PostMapping("/create")
  public ResponseEntity<CategoryResponse> createCategory(
      @Valid @RequestBody CategoryRequest categoryRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(categoryService.createCategory(categoryRequest));
  }

  @GetMapping
  public ResponseEntity<List<CategoryResponse>> findAll() {
    return ResponseEntity.status(HttpStatus.OK).body(categoryService.findAll());
  }

  @GetMapping("/{id}")
  public ResponseEntity<CategoryResponse> findById(@PathVariable Long id) {
    return ResponseEntity.status(HttpStatus.OK).body(categoryService.findById(id));
  }

  @GetMapping("/root-categories")
  public ResponseEntity<List<CategoryResponse>> findRootCategories() {
    return ResponseEntity.status(HttpStatus.OK).body(categoryService.findRootCategories());
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deactivateCategory(@PathVariable Long id) {
    categoryService.deactivateCategory(id);
    return ResponseEntity.noContent().build();
  }
}

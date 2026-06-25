package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.service.CategoryService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

  public static final String CATEGORY_WITH_ID = "Category with id ";
  public static final String NOT_FOUND = " not found";
  private final CategoryRepository categoryRepository;

  private CategoryResponse toResponse(Category category) {
    return new CategoryResponse(category.getId(), category.getName(), category.getActive());
  }

  @Override
  public CategoryResponse createCategory(CategoryRequest request) {
    if (categoryRepository.findByName(request.name()).isPresent()) {
      throw new ResourceAlreadyExistsException(
          "Category with name " + request.name() + " already exists");
    }
    Category category = new Category();
    category.setName(request.name());
    if (request.parentCategoryId() != null) {
      Category parent =
          categoryRepository
              .findById(request.parentCategoryId())
              .orElseThrow(
                  () ->
                      new ResourceNotFoundException(
                          CATEGORY_WITH_ID + request.parentCategoryId() + NOT_FOUND));
      category.setParentCategory(parent);
    }
    category.setActive(true);
    Category saved = categoryRepository.save(category);
    return toResponse(saved);
  }

  @Override
  public CategoryResponse findById(Long id) {
    return toResponse(
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_WITH_ID + id + NOT_FOUND)));
  }

  @Override
  public List<CategoryResponse> findAll() {
    return categoryRepository.findByActiveTrue().stream().map(this::toResponse).toList();
  }

  @Override
  public List<CategoryResponse> findRootCategories() {
    return categoryRepository.findByParentCategoryIsNullAndActiveTrue().stream()
        .map(this::toResponse)
        .toList();
  }

  @Override
  public void deactivateCategory(Long id) {
    Category category =
        categoryRepository
            .findById(id)
            .orElseThrow(() -> new ResourceNotFoundException(CATEGORY_WITH_ID + id + NOT_FOUND));
    category.setActive(false);
    categoryRepository.save(category);
  }
}

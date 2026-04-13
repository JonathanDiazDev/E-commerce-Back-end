package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse createCategory(CategoryRequest categoryRequest);
    CategoryResponse findById(Long id);
    List<CategoryResponse> findAll();
    List<CategoryResponse> findRootCategories();
    void deactivateCategory (Long id);
}

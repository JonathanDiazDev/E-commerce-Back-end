package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CategoryRepository;
import com.jonathan.ecommerce.service.CategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    private CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getActive()
        );
    }

    @Override
    public CategoryResponse createCategory(CategoryRequest request) {
        if (categoryRepository.findByName(request.name()).isPresent()) {
            throw new ResourceAlreadyExistsException("Category with name " + request.name() + " already exists");
        }
        Category category = new  Category();
        category.setName(request.name());
        if (request.parentCategoryId() != null) {
            Category parent = categoryRepository.findById(request.parentCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category with id " + request.parentCategoryId() + " not found"));
            category.setParentCategory(parent);
        }
        category.setActive(true);
        categoryRepository.save(category);
        return toResponse(category);
    }

    @Override
    public CategoryResponse findById(Long id) {
        return toResponse(categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found")));
    }

    @Override
    public List<CategoryResponse> findAll() {
        return categoryRepository.findByActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public List<CategoryResponse> findRootCategories() {
        return categoryRepository.findByParentCategoryIsNullAndActiveTrue()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    public void deactivateCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
        category.setActive(false);
        categoryRepository.save(category);
    }
}

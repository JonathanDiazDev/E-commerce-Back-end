package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;
import com.jonathan.ecommerce.entity.Category;
import com.jonathan.ecommerce.exception.ResourceAlreadyExistsException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.CategoryRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

  @Mock private CategoryRepository categoryRepository;

  @InjectMocks private CategoryServiceImpl categoryService;

  @Captor private ArgumentCaptor<Category> categoryCaptor;

  @Test
  void createCategory_Success() {
    CategoryRequest request = new CategoryRequest("Electronics", null);
    Category saved = createTestCategory(1L, "Electronics");

    when(categoryRepository.findByName("Electronics")).thenReturn(Optional.empty());
    when(categoryRepository.save(any(Category.class))).thenReturn(saved);

    CategoryResponse result = categoryService.createCategory(request);

    assertThat(result.name()).isEqualTo("Electronics");
    assertThat(result.active()).isTrue();
  }

  @Test
  void createCategory_WithParent_Success() {
    Category parent = createTestCategory(1L, "Parent");
    CategoryRequest request = new CategoryRequest("SubCategory", 1L);
    Category saved = createTestCategory(2L, "SubCategory");
    saved.setParentCategory(parent);

    when(categoryRepository.findByName("SubCategory")).thenReturn(Optional.empty());
    when(categoryRepository.findById(1L)).thenReturn(Optional.of(parent));
    when(categoryRepository.save(any(Category.class))).thenReturn(saved);

    CategoryResponse result = categoryService.createCategory(request);

    assertThat(result.name()).isEqualTo("SubCategory");

    // Verificación estricta: Nos aseguramos de que el servicio realmente le asignó el padre
    verify(categoryRepository).save(categoryCaptor.capture());
    Category categoryToSave = categoryCaptor.getValue();
    assertThat(categoryToSave.getParentCategory()).isNotNull();
    assertThat(categoryToSave.getParentCategory().getId()).isEqualTo(1L);
  }

  @Test
  void createCategory_AlreadyExists() {
    CategoryRequest request = new CategoryRequest("Electronics", null);
    Category existing = createTestCategory(null, "Electronics");

    when(categoryRepository.findByName("Electronics")).thenReturn(Optional.of(existing));

    assertThrows(
        ResourceAlreadyExistsException.class, () -> categoryService.createCategory(request));
    verify(categoryRepository, never()).save(any());
  }

  @Test
  void createCategory_ParentNotFound() {
    CategoryRequest request = new CategoryRequest("SubCategory", 999L);

    when(categoryRepository.findByName("SubCategory")).thenReturn(Optional.empty());
    when(categoryRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> categoryService.createCategory(request));
    verify(categoryRepository, never()).save(any());
  }

  @Test
  void findById_Success() {
    Category category = createTestCategory(1L, "Electronics");

    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));

    CategoryResponse result = categoryService.findById(1L);

    assertThat(result.name()).isEqualTo("Electronics");
  }

  @Test
  void findById_NotFound() {
    when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> categoryService.findById(1L));
  }

  @Test
  void findAll_Success() {
    Category cat1 = createTestCategory(1L, "Electronics");
    Category cat2 = createTestCategory(2L, "Books");

    when(categoryRepository.findByActiveTrue()).thenReturn(List.of(cat1, cat2));

    List<CategoryResponse> result = categoryService.findAll();

    assertThat(result).hasSize(2);
    assertThat(result.get(0).name()).isEqualTo("Electronics");
    assertThat(result.get(1).name()).isEqualTo("Books");
  }

  @Test
  void findRootCategories_Success() {
    Category cat = createTestCategory(1L, "Root");

    when(categoryRepository.findByParentCategoryIsNullAndActiveTrue()).thenReturn(List.of(cat));

    List<CategoryResponse> result = categoryService.findRootCategories();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).name()).isEqualTo("Root");
  }

  @Test
  void deactivateCategory_Success() {
    Category category = createTestCategory(1L, "Electronics");

    when(categoryRepository.findById(1L)).thenReturn(Optional.of(category));
    when(categoryRepository.save(any(Category.class))).thenReturn(category);

    categoryService.deactivateCategory(1L);

    assertThat(category.getActive()).isFalse(); // Ajustado a convención boolean estándar
    verify(categoryRepository).save(category);
  }

  @Test
  void deactivateCategory_NotFound() {
    when(categoryRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> categoryService.deactivateCategory(1L));
    verify(categoryRepository, never()).save(any());
  }

  // Método helper privado para limpiar la inicialización de entidades
  private Category createTestCategory(Long id, String name) {
    Category category = new Category();
    category.setId(id);
    category.setName(name);
    category.setActive(true);
    return category;
  }
}

package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.CategoryRequest;
import com.jonathan.ecommerce.dto.response.CategoryResponse;
import com.jonathan.ecommerce.service.CategoryService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
@ActiveProfiles("test")
@WithMockUser
class CategoryControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private CategoryService categoryService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void createCategory_ShouldReturn201() throws Exception {
    CategoryRequest request = new CategoryRequest("Electronics", null);
    CategoryResponse response = new CategoryResponse(1L, "Electronics", true);

    when(categoryService.createCategory(any(CategoryRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/categories/create")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.name").value("Electronics"));
  }

  @Test
  void findAll_ShouldReturn200() throws Exception {
    when(categoryService.findAll())
        .thenReturn(
            List.of(
                new CategoryResponse(1L, "Electronics", true),
                new CategoryResponse(2L, "Books", true)));

    mockMvc
        .perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Electronics"))
        .andExpect(jsonPath("$[1].name").value("Books"));
  }

  @Test
  void findById_ShouldReturn200() throws Exception {
    when(categoryService.findById(1L)).thenReturn(new CategoryResponse(1L, "Electronics", true));

    mockMvc
        .perform(get("/api/v1/categories/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Electronics"));
  }

  @Test
  void findRootCategories_ShouldReturn200() throws Exception {
    when(categoryService.findRootCategories())
        .thenReturn(List.of(new CategoryResponse(1L, "Root", true)));

    mockMvc
        .perform(get("/api/v1/categories/root-categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Root"));
  }

  @Test
  void deactivateCategory_ShouldReturn204() throws Exception {
    doNothing().when(categoryService).deactivateCategory(1L);

    mockMvc.perform(delete("/api/v1/categories/1").with(csrf())).andExpect(status().isNoContent());
  }
}

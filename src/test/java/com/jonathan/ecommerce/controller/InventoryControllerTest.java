package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jonathan.ecommerce.dto.response.InventoryResponse;
import com.jonathan.ecommerce.dto.response.MovementResponse;
import com.jonathan.ecommerce.entity.enums.InventoryStatus;
import com.jonathan.ecommerce.entity.enums.MovementType;
import com.jonathan.ecommerce.service.InventoryService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(InventoryController.class)
@ActiveProfiles("test")
@WithMockUser
class InventoryControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private InventoryService inventoryService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void getDetails_ShouldReturn200() throws Exception {
    InventoryResponse response = new InventoryResponse(1L, 100, InventoryStatus.IN_STOCK);
    when(inventoryService.getStockDetails(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/inventories/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(100));
  }

  @Test
  void getProductHistory_ShouldReturn200() throws Exception {
    MovementResponse movement =
        new MovementResponse(
            1L, "Product", MovementType.SALE, -10, "Sale", Instant.now(), "2024-01-01");
    Page<MovementResponse> page = new PageImpl<>(java.util.List.of(movement));
    when(inventoryService.getMovementHistory(anyLong(), any())).thenReturn(page);

    mockMvc
        .perform(
            get("/api/v1/inventories/product/1/history")
                .param("page", "0")
                .param("size", "10")
                .param("sortBy", "date")
                .param("direction", "DESC"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].productName").value("Product"));
  }

  @Test
  void addStock_ShouldReturn200() throws Exception {
    InventoryResponse response = new InventoryResponse(1L, 150, InventoryStatus.IN_STOCK);
    when(inventoryService.addStock(eq(1L), eq(50), eq("Restock"))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/inventories/1/add")
                .with(csrf())
                .param("quantity", "50")
                .param("reason", "Restock"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(150));
  }

  @Test
  void deductStock_ShouldReturn200() throws Exception {
    InventoryResponse response = new InventoryResponse(1L, 50, InventoryStatus.IN_STOCK);
    when(inventoryService.deductStock(eq(1L), eq(50), eq("Order #123"))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/inventories/1/deduct")
                .with(csrf())
                .param("quantity", "50")
                .param("reason", "Order #123"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.quantity").value(50));
  }

  @Test
  void updateStatus_ShouldReturn200() throws Exception {
    InventoryResponse response = new InventoryResponse(1L, 100, InventoryStatus.OUT_OF_STOCK);
    when(inventoryService.updateInventoryStatus(eq(1L), eq(InventoryStatus.OUT_OF_STOCK)))
        .thenReturn(response);

    mockMvc
        .perform(patch("/api/v1/inventories/1/status").with(csrf()).param("status", "OUT_OF_STOCK"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.inventoryStatus").value("OUT_OF_STOCK"));
  }
}

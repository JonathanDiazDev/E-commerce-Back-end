package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.OrderRequest;
import com.jonathan.ecommerce.dto.response.OrderItemResponse;
import com.jonathan.ecommerce.dto.response.OrderResponse;
import com.jonathan.ecommerce.service.OrderService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import java.math.BigDecimal;
import java.time.Instant;
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

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
@WithMockUser
class OrderControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private OrderService orderService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void checkout_ShouldReturn201() throws Exception {
    OrderRequest request = new OrderRequest(1L, "pm_card_visa");
    OrderResponse response =
        new OrderResponse(1L, Instant.now(), "PAID", BigDecimal.valueOf(500), List.of());

    when(orderService.placeOrder(any(OrderRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/orders/checkout")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PAID"));
  }

  @Test
  void getAllOrders_ShouldReturn200() throws Exception {
    OrderResponse response =
        new OrderResponse(1L, Instant.now(), "PAID", BigDecimal.valueOf(500), List.of());
    when(orderService.getOrders()).thenReturn(List.of(response));

    mockMvc
        .perform(get("/api/v1/orders"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].status").value("PAID"));
  }

  @Test
  void getOrderById_ShouldReturn200() throws Exception {
    OrderResponse response =
        new OrderResponse(
            1L,
            Instant.now(),
            "PAID",
            BigDecimal.valueOf(500),
            List.of(new OrderItemResponse(1L, "Product1", 2, BigDecimal.valueOf(250))));
    when(orderService.getOrderById(1L)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/orders/1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].productName").value("Product1"));
  }
}

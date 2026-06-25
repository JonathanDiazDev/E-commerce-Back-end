package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.RefundRequest;
import com.jonathan.ecommerce.service.RefundService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(RefundController.class)
@ActiveProfiles("test")
@WithMockUser
class RefundControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private RefundService refundService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void requestRefund_ShouldReturn200() throws Exception {
    RefundRequest request = new RefundRequest(1L, "Item damaged");
    doNothing().when(refundService).processRefund(any(RefundRequest.class));

    mockMvc
        .perform(
            post("/api/v1/refunds")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}

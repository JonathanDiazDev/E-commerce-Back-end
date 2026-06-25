package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.PaymentRequest;
import com.jonathan.ecommerce.service.PaymentService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentController.class)
@ActiveProfiles("test")
@WithMockUser
class PaymentControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private PaymentService paymentService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void processPayment_ShouldReturn200() throws Exception {
    PaymentRequest request = new PaymentRequest(1L, "pm_card_visa", BigDecimal.valueOf(500));
    doNothing().when(paymentService).confirmPayment(any(PaymentRequest.class));

    mockMvc
        .perform(
            post("/api/v1/payments/process")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Pago procesado con éxito"));
  }
}

package com.jonathan.ecommerce.controller;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jonathan.ecommerce.service.StripeWebhookService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PaymentWebhookController.class)
@ActiveProfiles("test")
@WithMockUser
class PaymentWebhookControllerTest {

  @Autowired private MockMvc mockMvc;
  @MockitoBean private StripeWebhookService stripeWebhookService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void handleStripeWebhook_ValidSignature_Returns200() throws Exception {
    when(stripeWebhookService.validateWebhookSignature(anyString(), anyString())).thenReturn(true);

    mockMvc
        .perform(
            post("/api/v1/webhooks/stripe")
                .with(csrf())
                .content("{\"type\":\"payment_intent.succeeded\"}")
                .header("Stripe-Signature", "valid_sig"))
        .andExpect(status().isOk());
  }

  @Test
  void handleStripeWebhook_InvalidSignature_Returns400() throws Exception {
    when(stripeWebhookService.validateWebhookSignature(anyString(), anyString())).thenReturn(false);

    mockMvc
        .perform(
            post("/api/v1/webhooks/stripe")
                .with(csrf())
                .content("{\"type\":\"payment_intent.succeeded\"}")
                .header("Stripe-Signature", "invalid_sig"))
        .andExpect(status().isBadRequest());
  }
}

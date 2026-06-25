package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.ForgotPasswordRequest;
import com.jonathan.ecommerce.dto.request.ResetPasswordRequest;
import com.jonathan.ecommerce.service.PasswordResetService;
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

@WebMvcTest(PasswordResetController.class)
@ActiveProfiles("test")
@WithMockUser
class PasswordResetControllerTest {

  @Autowired private MockMvc mockMvc;
  @Autowired private ObjectMapper objectMapper;
  @MockitoBean private PasswordResetService passwordResetService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void forgotPassword_ShouldReturn200() throws Exception {
    ForgotPasswordRequest request = new ForgotPasswordRequest("test@test.com");
    when(passwordResetService.forgotPassword(any(ForgotPasswordRequest.class)))
        .thenReturn("Reset email sent");

    mockMvc
        .perform(
            post("/api/v1/password-reset/forgot-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(content().string("Reset email sent"));
  }

  @Test
  void resetPassword_ShouldReturn200() throws Exception {
    ResetPasswordRequest request = new ResetPasswordRequest("token123", "NewPass123!!");
    doNothing().when(passwordResetService).resetPassword(any(ResetPasswordRequest.class));

    mockMvc
        .perform(
            post("/api/v1/password-reset/reset-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk());
  }
}

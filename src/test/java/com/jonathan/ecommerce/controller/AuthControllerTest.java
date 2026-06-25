package com.jonathan.ecommerce.controller;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.ChangePasswordRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.dto.response.SessionResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;
import com.jonathan.ecommerce.service.AuthService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@ActiveProfiles("test")
@WithMockUser
class AuthControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private AuthService authService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;
  @MockitoBean private TokenBlacklistService tokenBlacklistService;

  @Test
  void register_ShouldReturn201() throws Exception {
    UserRequest request = new UserRequest("Test", "test@test.com", "StrongPass1!");
    UserResponse response =
        new UserResponse(1L, "test@test.com", "Test", "USER", LocalDateTime.now());

    when(authService.register(any(UserRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/register")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void login_ShouldReturn200() throws Exception {
    AuthRequest request = new AuthRequest("test@test.com", "password");
    AuthUserResponse response = new AuthUserResponse("test@test.com", "Test", "USER");

    when(authService.login(any(AuthRequest.class), any(HttpServletResponse.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void refreshToken_ShouldReturn200() throws Exception {
    AuthUserResponse response = new AuthUserResponse("test@test.com", "Test", "USER");

    when(authService.refreshToken(anyString(), any(HttpServletResponse.class)))
        .thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/auth/refresh")
                .with(csrf())
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "token123")))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void logout_ShouldReturn204() throws Exception {
    doNothing().when(authService).logout(anyString(), anyString(), any(HttpServletResponse.class));

    mockMvc
        .perform(
            post("/api/v1/auth/logout")
                .with(csrf())
                .cookie(new jakarta.servlet.http.Cookie("refresh_token", "rt"))
                .cookie(new jakarta.servlet.http.Cookie("access_token", "at")))
        .andExpect(status().isNoContent());
  }

  @Test
  void logoutAll_ShouldReturn204() throws Exception {
    doNothing().when(authService).logoutAll();

    mockMvc.perform(post("/api/v1/auth/logout-all").with(csrf())).andExpect(status().isNoContent());
  }

  @Test
  void changePassword_ShouldReturn204() throws Exception {
    ChangePasswordRequest request = new ChangePasswordRequest("OldPass123!!", "NewPass123!!");
    doNothing()
        .when(authService)
        .changePassword(any(ChangePasswordRequest.class), any(HttpServletResponse.class));

    mockMvc
        .perform(
            patch("/api/v1/auth/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNoContent());
  }

  @Test
  void me_ShouldReturn200() throws Exception {
    AuthUserResponse response = new AuthUserResponse("test@test.com", "Test", "USER");
    when(authService.me()).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/auth/me"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.email").value("test@test.com"));
  }

  @Test
  void sessions_ShouldReturn200() throws Exception {
    SessionResponse session =
        new SessionResponse(
            UUID.randomUUID(),
            Instant.now(),
            Instant.now().plusSeconds(3600),
            "Mozilla",
            "127.0.0.1");
    when(authService.getActiveSessions()).thenReturn(List.of(session));

    mockMvc
        .perform(get("/api/v1/auth/sessions"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].userAgent").value("Mozilla"));
  }
}

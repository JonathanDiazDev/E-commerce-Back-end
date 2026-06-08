package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.ChangePasswordRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.dto.response.SessionResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;
import com.jonathan.ecommerce.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  @PostMapping("/register")
  public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
  }

  @PostMapping("/login")
  public ResponseEntity<AuthUserResponse> login(
      @Valid @RequestBody AuthRequest request, HttpServletResponse response) {
    return ResponseEntity.ok(authService.login(request, response));
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthUserResponse> refreshToken(
      @CookieValue("refresh_token") String refreshToken, HttpServletResponse response) {
    return ResponseEntity.ok(authService.refreshToken(refreshToken, response));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(
      @CookieValue("refresh_token") String refreshToken,
      @CookieValue("access_token") String accessToken,
      HttpServletResponse response) {
    authService.logout(refreshToken, accessToken, response);
    return ResponseEntity.noContent().build();
  }

  @PostMapping("/logout-all")
  public ResponseEntity<Void> logoutAll() {

    authService.logoutAll();
    return ResponseEntity.noContent().build();
  }

  @PatchMapping("/change-password")
  public ResponseEntity<Void> changePassword(
      @Valid @RequestBody ChangePasswordRequest request, HttpServletResponse response) {
    authService.changePassword(request, response);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/me")
  public ResponseEntity<AuthUserResponse> me() {
    return ResponseEntity.ok(authService.me());
  }

  @GetMapping("/sessions")
  public ResponseEntity<List<SessionResponse>> sessions() {
    return ResponseEntity.ok(authService.getActiveSessions());
  }
}

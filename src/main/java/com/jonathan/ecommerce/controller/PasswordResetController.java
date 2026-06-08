package com.jonathan.ecommerce.controller;

import com.jonathan.ecommerce.dto.request.ForgotPasswordRequest;
import com.jonathan.ecommerce.dto.request.ResetPasswordRequest;
import com.jonathan.ecommerce.service.PasswordResetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/password-reset")
public class PasswordResetController {
  private final PasswordResetService passwordResetService;

  @PostMapping("/forgot-password")
  public ResponseEntity<String> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
    return ResponseEntity.ok().body(passwordResetService.forgotPassword(request));
  }

  @PostMapping("/reset-password")
  public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
    passwordResetService.resetPassword(request);
    return ResponseEntity.ok().build();
  }
}

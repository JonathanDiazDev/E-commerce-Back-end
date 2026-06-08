package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.ForgotPasswordRequest;
import com.jonathan.ecommerce.dto.request.ResetPasswordRequest;
import com.jonathan.ecommerce.entity.PasswordResetToken;

public interface PasswordResetService {

  String forgotPassword(ForgotPasswordRequest request);

  PasswordResetToken validateToken(String token);

  void resetPassword(ResetPasswordRequest request);
}

package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;

public interface AuthService {
  UserResponse register(UserRequest request);

  AuthResponse login(AuthRequest request);

  AuthResponse refreshToken(String refreshToken);

  void logout(String refreshToken, String accessToken);

  void logoutAll(String email);
}

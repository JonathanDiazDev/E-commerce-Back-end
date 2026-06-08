package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.ChangePasswordRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.dto.response.SessionResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;

public interface AuthService {
  UserResponse register(UserRequest request);

  AuthUserResponse login(AuthRequest request, HttpServletResponse response);

  AuthUserResponse refreshToken(String refreshToken, HttpServletResponse response);

  void logout(String refreshToken, String accessToken, HttpServletResponse response);

  void logoutAll();

  void changePassword(ChangePasswordRequest request, HttpServletResponse response);

  AuthUserResponse me();

  List<SessionResponse> getActiveSessions();
}

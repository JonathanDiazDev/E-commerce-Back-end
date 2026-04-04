package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.AuthRequest;
import com.jonathan.ecommerce.dto.AuthResponse;
import com.jonathan.ecommerce.dto.UserRequest;
import com.jonathan.ecommerce.dto.UserResponse;

public interface UserService{
    UserResponse register(UserRequest request);
    AuthResponse login(AuthRequest request);
}

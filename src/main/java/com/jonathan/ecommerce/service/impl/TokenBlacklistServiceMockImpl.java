package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.service.TokenBlacklistService;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Service
@Profile("test")
public class TokenBlacklistServiceMockImpl implements TokenBlacklistService {

  @Override
  public void blacklistToken(String token, Long expirationMillis) {
    // No-op for tests
  }

  @Override
  public boolean isTokenBlacklisted(String token) {
    // Always return false for tests
    return false;
  }
}

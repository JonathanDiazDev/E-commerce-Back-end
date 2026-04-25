package com.jonathan.ecommerce.service;

public interface TokenBlacklistService {

  void blacklistToken(String token, Long expirationMillis);

  boolean isTokenBlacklisted(String token);
}

package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.service.TokenBlacklistService;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

  private final RedisTemplate<String, Object> redisTemplate;
  private static final String BLACKLIST_PREFIX = "blacklist:";

  @Override
  public void blacklistToken(String token, Long expirationMillis) {
    String key = BLACKLIST_PREFIX + token;
    redisTemplate.opsForValue().set(key, "revoked", expirationMillis, TimeUnit.MILLISECONDS);
  }

  @Override
  public boolean isTokenBlacklisted(String token) {
    String key = BLACKLIST_PREFIX + token;
    return redisTemplate.hasKey(key);
  }
}

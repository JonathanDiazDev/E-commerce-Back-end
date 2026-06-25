package com.jonathan.ecommerce.service.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceImplTest {

  @Mock private RedisTemplate<String, Object> redisTemplate;
  @Mock private ValueOperations<String, Object> valueOperations;

  @InjectMocks private TokenBlacklistServiceImpl tokenBlacklistService;

  @Test
  void blacklistToken_Success() {
    when(redisTemplate.opsForValue()).thenReturn(valueOperations);

    tokenBlacklistService.blacklistToken("token123", 3600000L);

    verify(valueOperations).set("blacklist:token123", "revoked", 3600000L, TimeUnit.MILLISECONDS);
  }

  @Test
  void isTokenBlacklisted_ReturnsTrue() {
    when(redisTemplate.hasKey("blacklist:token123")).thenReturn(true);

    boolean result = tokenBlacklistService.isTokenBlacklisted("token123");

    org.assertj.core.api.Assertions.assertThat(result).isTrue();
  }

  @Test
  void isTokenBlacklisted_ReturnsFalse() {
    when(redisTemplate.hasKey("blacklist:token456")).thenReturn(false);

    boolean result = tokenBlacklistService.isTokenBlacklisted("token456");

    org.assertj.core.api.Assertions.assertThat(result).isFalse();
  }
}

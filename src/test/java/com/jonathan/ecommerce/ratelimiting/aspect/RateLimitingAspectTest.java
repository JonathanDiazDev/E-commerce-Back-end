package com.jonathan.ecommerce.ratelimiting.aspect;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.ratelimiting.annotation.RateLimited;
import com.jonathan.ecommerce.ratelimiting.config.RateLimitingProperties;
import com.jonathan.ecommerce.ratelimiting.exception.RateLimitExceededException;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import java.util.function.Supplier;
import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimitingAspectTest {

  @Mock private ProxyManager<byte[]> proxyManager;
  @Mock private RateLimitingProperties rateLimitingProperties;
  @Mock private ProceedingJoinPoint joinPoint;
  @Mock private RateLimited rateLimited;
  @Mock private RemoteBucketBuilder remoteBucketBuilder;

  @InjectMocks private RateLimitingAspect rateLimitingAspect;

  private RateLimitingProperties.EndpointLimit endpointLimit;

  @BeforeEach
  void setUp() {
    endpointLimit = new RateLimitingProperties.EndpointLimit();
    endpointLimit.setCapacity(10);
    endpointLimit.setRefillTokens(10);
    endpointLimit.setRefillMinutes(1);
    endpointLimit.setUserCapacity(5);
    endpointLimit.setUserRefillTokens(5);
    endpointLimit.setUserRefillMinutes(1);
    endpointLimit.setIpCapacity(100);
    endpointLimit.setIpRefillTokens(100);
    endpointLimit.setIpRefillMinutes(1);
    endpointLimit.setFailOpen(false);
  }

  @Test
  void handleRateLimit_AllChecksPass_Proceeds() throws Throwable {
    when(rateLimited.value()).thenReturn("/api/test");
    when(rateLimitingProperties.getLimitForEndpoint("/api/test")).thenReturn(endpointLimit);
    when(proxyManager.builder()).thenReturn(remoteBucketBuilder);

    BucketProxy bucket = mock(BucketProxy.class);
    when(bucket.tryConsume(1)).thenReturn(true);
    when(remoteBucketBuilder.build(any(), any(Supplier.class))).thenReturn(bucket);

    when(joinPoint.proceed()).thenReturn("success");

    Object result = rateLimitingAspect.handleRateLimit(joinPoint, rateLimited);

    assertThat(result).isEqualTo("success");
    verify(joinPoint).proceed();
  }

  @Test
  void handleRateLimit_EndpointLimitExceeded_ThrowsException() throws Throwable {
    when(rateLimited.value()).thenReturn("/api/test");
    when(rateLimitingProperties.getLimitForEndpoint("/api/test")).thenReturn(endpointLimit);
    when(proxyManager.builder()).thenReturn(remoteBucketBuilder);

    BucketProxy bucket = mock(BucketProxy.class);
    when(bucket.tryConsume(1)).thenReturn(false);
    when(remoteBucketBuilder.build(any(), any(Supplier.class))).thenReturn(bucket);

    assertThrows(
        RateLimitExceededException.class,
        () -> rateLimitingAspect.handleRateLimit(joinPoint, rateLimited));

    verify(joinPoint, never()).proceed();
  }

  @Test
  void handleRateLimit_FailOpen_ProceedsOnError() throws Throwable {
    endpointLimit.setFailOpen(true);

    when(rateLimited.value()).thenReturn("/api/test");
    when(rateLimitingProperties.getLimitForEndpoint("/api/test")).thenReturn(endpointLimit);
    when(proxyManager.builder()).thenReturn(remoteBucketBuilder);

    BucketProxy bucket = mock(BucketProxy.class);
    when(bucket.tryConsume(1)).thenThrow(new RuntimeException("Redis down"));
    when(remoteBucketBuilder.build(any(), any(Supplier.class))).thenReturn(bucket);

    when(joinPoint.proceed()).thenReturn("success");

    Object result = rateLimitingAspect.handleRateLimit(joinPoint, rateLimited);

    assertThat(result).isEqualTo("success");
    verify(joinPoint).proceed();
  }

  @Test
  void handleRateLimit_FailClosed_ThrowsOnError() throws Throwable {
    endpointLimit.setFailOpen(false);

    when(rateLimited.value()).thenReturn("/api/test");
    when(rateLimitingProperties.getLimitForEndpoint("/api/test")).thenReturn(endpointLimit);
    when(proxyManager.builder()).thenReturn(remoteBucketBuilder);

    BucketProxy bucket = mock(BucketProxy.class);
    when(bucket.tryConsume(1)).thenThrow(new RuntimeException("Redis down"));
    when(remoteBucketBuilder.build(any(), any(Supplier.class))).thenReturn(bucket);

    assertThrows(
        RateLimitExceededException.class,
        () -> rateLimitingAspect.handleRateLimit(joinPoint, rateLimited));

    verify(joinPoint, never()).proceed();
  }
}

package com.jonathan.ecommerce.ratelimiting.aspect;

import com.jonathan.ecommerce.ratelimiting.annotation.RateLimited;
import com.jonathan.ecommerce.ratelimiting.config.RateLimitingProperties;
import com.jonathan.ecommerce.ratelimiting.exception.RateLimitExceededException;
import com.jonathan.ecommerce.util.RequestContextUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

/**
 * AOP aspect implementing three-layer rate limiting:
 *
 * <p>1. ENDPOINT LEVEL: Global limit per endpoint (e.g., /login: 5 req/min for all users) 2. USER
 * LEVEL: Per-authenticated-user limit (e.g., user can make 100 API calls/min) 3. IP LEVEL:
 * Per-source-IP limit (e.g., 500 req/min from any single IP)
 *
 * <p>All three checks must pass. If any is exceeded → 429 Too Many Requests.
 */
@Aspect
@Component
@Slf4j
public class RateLimitingAspect {
  private final ProxyManager<byte[]> proxyManager;
  private final RateLimitingProperties rateLimitProperties;

  public RateLimitingAspect(
      ProxyManager<byte[]> proxyManager, RateLimitingProperties rateLimitProperties) {
    this.proxyManager = proxyManager;
    this.rateLimitProperties = rateLimitProperties;
  }

  @Around("@annotation(rateLimited)")
  public Object handleRateLimit(ProceedingJoinPoint joinPoint, RateLimited rateLimited)
      throws Throwable {
    // We obtain the configuration according to the endpoint defined in the annotation
    String endpointKey = rateLimited.value();
    RateLimitingProperties.EndpointLimit config =
        rateLimitProperties.getLimitForEndpoint(endpointKey);

    try {
      // 1. Check endpoint-level limit
      if (!checkEndpointLimit(endpointKey, config)) {
        throw new RateLimitExceededException("Endpoint limit exceeded: " + endpointKey);
      }

      // 2. Check user-level limit
      if (config.getUserCapacity() > 0 && !checkUserLimit(endpointKey, config)) {
        throw new RateLimitExceededException("User limit exceeded");
      }

      // 3. Check IP-level limit
      if (config.getIpCapacity() > 0 && !checkIpLimit(config)) {
        throw new RateLimitExceededException("IP limit exceeded");
      }

      return joinPoint.proceed();

    } catch (Exception ex) {
      if (config.getFailOpen()) {
        log.warn(
            "Rate limit check failed (fail-open). Allowing request. Error: {}", ex.getMessage());
        return joinPoint.proceed();
      } else {
        log.error("Rate limit service unavailable. Rejecting.", ex);
        throw new RateLimitExceededException("Service unavailable");
      }
    }
  }

  private boolean checkEndpointLimit(
      String endpointKey, RateLimitingProperties.EndpointLimit config) {
    byte[] key = ("endpoint:" + endpointKey).getBytes(StandardCharsets.UTF_8);
    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                buildConfig(
                    config.getCapacity(), config.getRefillTokens(), config.getRefillMinutes()));
    return bucket.tryConsume(1);
  }

  private boolean checkUserLimit(String endpointKey, RateLimitingProperties.EndpointLimit config) {
    String userId = extractUserId();
    if (userId == null) userId = "anonymous";

    byte[] key = ("user:" + userId + ":" + endpointKey).getBytes(StandardCharsets.UTF_8);
    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                buildConfig(
                    config.getUserCapacity(),
                    config.getUserRefillTokens(),
                    config.getUserRefillMinutes()));
    return bucket.tryConsume(1);
  }

  private boolean checkIpLimit(RateLimitingProperties.EndpointLimit config) {
    String clientIp = extractClientIp();
    byte[] key = ("ip:" + clientIp).getBytes(StandardCharsets.UTF_8);
    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                buildConfig(
                    config.getIpCapacity(),
                    config.getIpRefillTokens(),
                    config.getIpRefillMinutes()));
    return bucket.tryConsume(1);
  }

  private Supplier<BucketConfiguration> buildConfig(int capacity, int tokens, int minutes) {
    return () ->
        BucketConfiguration.builder()
            .addLimit(
                Bandwidth.classic(capacity, Refill.intervally(tokens, Duration.ofMinutes(minutes))))
            .build();
  }

  private String extractUserId() {
    return RequestContextUtil.getUserId();
  }

  private String extractClientIp() {
    return RequestContextUtil.getClientIp();
  }
}

package com.jonathan.ecommerce.ratelimiting.filter;

import com.jonathan.ecommerce.ratelimiting.config.RateLimitingProperties;
import com.jonathan.ecommerce.util.RequestContextUtil;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Filter-based rate limiting integrated into Spring Security filter chain.
 *
 * <p>Runs AFTER JWT authentication, checks three-layer rate limits: 1. Endpoint limit (global) 2.
 * User limit (per-authenticated-user) 3. IP limit (per-source-IP)
 *
 * <p>Configuration is centralized in application.yml, no annotations needed on controllers.
 *
 * <p>Place this in the SecurityFilterChain AFTER the JwtAuthenticationFilter: @Bean public
 * SecurityFilterChain securityFilterChain(HttpSecurity http, RateLimitingFilter rateLimitFilter) {
 * http.addFilterAfter(rateLimitFilter, JwtAuthenticationFilter.class); // ... other config }
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class RateLimitingFilter extends OncePerRequestFilter {

  private final ProxyManager<byte[]> proxyManager;
  private final RateLimitingProperties rateLimitingProperties;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    // Skip if rate limiting is disabled
    if (!rateLimitingProperties.isEnabled()) {
      filterChain.doFilter(request, response);
      return;
    }

    String requestPath = request.getRequestURI();
    RateLimitingProperties.EndpointLimit limit =
        rateLimitingProperties.getLimitForEndpoint(requestPath);

    try {
      // 1. Check endpoint-level limit (global)
      if (!checkEndpointLimit(requestPath, limit)) {
        sendRateLimitResponse(response, 429, "Endpoint rate limit exceeded");
        return;
      }

      // 2. Check user-level limit (per-authenticated-user)
      if (limit.getUserCapacity() > 0 && !checkUserLimit(requestPath, limit)) {
        sendRateLimitResponse(response, 429, "User rate limit exceeded");
        return;
      }

      // 3. Check IP-level limit (per-source-IP)
      if (limit.getIpCapacity() > 0 && !checkIpLimit(requestPath, limit)) {
        sendRateLimitResponse(response, 429, "IP rate limit exceeded");
        return;
      }

      // All checks passed, proceed
      filterChain.doFilter(request, response);

    } catch (Exception ex) {
      if (limit.getFailOpen()) {
        // Fail-open: allow request through
        log.warn(
            "Rate limiting check failed (fail-open mode). Request: {} Error: {}",
            requestPath,
            ex.getMessage());
        filterChain.doFilter(request, response);
      } else {
        // Fail-closed: reject request
        log.error("Rate limiting service unavailable. Rejecting request: {}", requestPath, ex);
        sendRateLimitResponse(response, 429, "Rate limiting service temporarily unavailable");
      }
    }
  }

  /** Check endpoint-level limit (global, shared by all users) */
  private boolean checkEndpointLimit(String path, RateLimitingProperties.EndpointLimit limit) {
    byte[] key = ("endpoint:" + path).getBytes(StandardCharsets.UTF_8);

    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                () ->
                    BucketConfiguration.builder()
                        .addLimit(
                            Bandwidth.classic(
                                limit.getCapacity(),
                                Refill.intervally(
                                    limit.getRefillTokens(),
                                    Duration.ofMinutes(limit.getRefillMinutes()))))
                        .build());

    boolean allowed = bucket.tryConsume(1);

    if (allowed) {
      log.debug("Endpoint rate limit check passed for: {}", path);
    } else {
      log.debug("Endpoint rate limit exceeded for: {}", path);
    }

    return allowed;
  }

  /** Check user-level limit (per-authenticated-user) */
  private boolean checkUserLimit(String path, RateLimitingProperties.EndpointLimit limit) {
    String userId = RequestContextUtil.getUserId();

    // If not authenticated, use "anonymous" key
    if (userId == null || userId.isEmpty()) {
      userId = "anonymous";
    }

    byte[] key = ("user:" + userId + ":" + path).getBytes(StandardCharsets.UTF_8);

    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                () ->
                    BucketConfiguration.builder()
                        .addLimit(
                            Bandwidth.classic(
                                limit.getUserCapacity(),
                                Refill.intervally(
                                    limit.getUserRefillTokens(),
                                    Duration.ofMinutes(limit.getUserRefillMinutes()))))
                        .build());

    boolean allowed = bucket.tryConsume(1);

    if (allowed) {
      log.debug("User rate limit check passed for userId: {} path: {}", userId, path);
    } else {
      log.debug("User rate limit exceeded for userId: {} path: {}", userId, path);
    }

    return allowed;
  }

  /** Check IP-level limit (per-source-IP) */
  private boolean checkIpLimit(String path, RateLimitingProperties.EndpointLimit limit) {
    String clientIp = RequestContextUtil.getClientIp();

    if (clientIp == null || clientIp.isEmpty()) {
      clientIp = "unknown";
    }

    byte[] key = ("ip:" + clientIp).getBytes(StandardCharsets.UTF_8);

    Bucket bucket =
        proxyManager
            .builder()
            .build(
                key,
                () ->
                    BucketConfiguration.builder()
                        .addLimit(
                            Bandwidth.classic(
                                limit.getIpCapacity(),
                                Refill.intervally(
                                    limit.getIpRefillTokens(),
                                    Duration.ofMinutes(limit.getIpRefillMinutes()))))
                        .build());

    boolean allowed = bucket.tryConsume(1);

    if (allowed) {
      log.debug("IP rate limit check passed for ip: {} path: {}", clientIp, path);
    } else {
      log.debug("IP rate limit exceeded for ip: {} path: {}", clientIp, path);
    }

    return allowed;
  }

  /** Send HTTP 429 response with JSON body */
  private void sendRateLimitResponse(HttpServletResponse response, int statusCode, String message)
      throws IOException {

    response.setStatus(statusCode);
    response.setHeader("Content-Type", "application/json");
    response.setHeader("Retry-After", "60");

    String jsonBody =
        """
        {
          "status": %d,
          "error": "Too Many Requests",
          "message": "%s"
        }
        """
            .formatted(statusCode, message);

    response.getWriter().write(jsonBody);
    response.getWriter().flush();
  }

  @Override
  protected boolean shouldNotFilter(HttpServletRequest request) throws ServletException {
    // Skip rate limiting for health checks and public endpoints
    String path = request.getRequestURI();
    return path.startsWith("/api/v1/webhooks/")
        || path.equals("/health")
        || path.equals("/actuator/health");
  }
}

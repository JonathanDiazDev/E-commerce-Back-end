package com.jonathan.ecommerce.ratelimiting.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** Rate limiting configuration loaded from application.yml */
@Data
@Component
@ConfigurationProperties(prefix = "app.rate-limiting")
public class RateLimitingProperties {

  private boolean enabled = true;

  /** Default rate limits applied to all endpoints not explicitly configured */
  private Defaults defaults = new Defaults();

  /**
   * Per-endpoint overrides Key: URI path pattern (e.g., "/api/v1/auth/login") Value:
   * Endpoint-specific limits
   */
  private Map<String, EndpointLimit> endpoints = new HashMap<>();

  @Data
  public static class Defaults {
    private int endpointCapacity = 100;
    private int endpointRefillMinutes = 1;

    private int userCapacity = 500;
    private int userRefillMinutes = 1;

    private int ipCapacity = 1000;
    private int ipRefillMinutes = 5;
  }

  @Data
  public static class EndpointLimit {
    // Endpoint-level limit
    private Integer capacity;
    private Integer refillMinutes;
    private Integer refillTokens;

    // User-level limit (0 = disabled)
    private Integer userCapacity = 0;
    private Integer userRefillMinutes = 1;
    private Integer userRefillTokens;

    // IP-level limit (0 = disabled)
    private Integer ipCapacity = 0;
    private Integer ipRefillMinutes = 5;
    private Integer ipRefillTokens;

    // Fail strategy
    private Boolean failOpen;
  }

  /**
   * Get limit configuration for a given endpoint path. Returns endpoint-specific config if found,
   * otherwise returns defaults.
   */
  public EndpointLimit getLimitForEndpoint(String path) {
    // Try exact match first
    if (endpoints.containsKey(path)) {
      return mergeWithDefaults(endpoints.get(path));
    }

    // Try pattern matching (e.g., "/api/v1/auth/*" matches "/api/v1/auth/login")
    for (Map.Entry<String, EndpointLimit> entry : endpoints.entrySet()) {
      if (pathMatches(entry.getKey(), path)) {
        return mergeWithDefaults(entry.getValue());
      }
    }

    // Return defaults
    return createDefaultLimit();
  }

  /** Merge endpoint-specific limits with defaults (endpoint-specific takes precedence) */
  private EndpointLimit mergeWithDefaults(EndpointLimit specific) {
    EndpointLimit merged = new EndpointLimit();

    // Endpoint level
    merged.setCapacity(
        specific.getCapacity() != null ? specific.getCapacity() : defaults.getEndpointCapacity());
    merged.setRefillMinutes(
        specific.getRefillMinutes() != null
            ? specific.getRefillMinutes()
            : defaults.getEndpointRefillMinutes());
    merged.setRefillTokens(
        merged.getRefillTokens() != null ? merged.getRefillTokens() : merged.getCapacity());

    // User level
    merged.setUserCapacity(
        specific.getUserCapacity() != null
            ? specific.getUserCapacity()
            : defaults.getUserCapacity());
    merged.setUserRefillMinutes(
        specific.getUserRefillMinutes() != null
            ? specific.getUserRefillMinutes()
            : defaults.getUserRefillMinutes());
    merged.setUserRefillTokens(
        merged.getUserRefillTokens() != null
            ? merged.getUserRefillTokens()
            : merged.getUserCapacity());

    // IP level
    merged.setIpCapacity(
        specific.getIpCapacity() != null ? specific.getIpCapacity() : defaults.getIpCapacity());
    merged.setIpRefillMinutes(
        specific.getIpRefillMinutes() != null
            ? specific.getIpRefillMinutes()
            : defaults.getIpRefillMinutes());
    merged.setIpRefillTokens(
        merged.getIpRefillTokens() != null ? merged.getIpRefillTokens() : merged.getIpCapacity());

    merged.setFailOpen(specific.getFailOpen() != null ? specific.getFailOpen() : true);

    return merged;
  }

  /** Create a limit object from defaults (used when endpoint not explicitly configured) */
  private EndpointLimit createDefaultLimit() {
    EndpointLimit limit = new EndpointLimit();
    limit.setCapacity(defaults.getEndpointCapacity());
    limit.setRefillMinutes(defaults.getEndpointRefillMinutes());
    limit.setRefillTokens(defaults.getEndpointCapacity());
    limit.setUserCapacity(defaults.getUserCapacity());
    limit.setUserRefillMinutes(defaults.getUserRefillMinutes());
    limit.setUserRefillTokens(defaults.getUserCapacity());
    limit.setIpCapacity(defaults.getIpCapacity());
    limit.setIpRefillMinutes(defaults.getIpRefillMinutes());
    limit.setIpRefillTokens(defaults.getIpCapacity());
    limit.setFailOpen(true);
    return limit;
  }

  /**
   * Simple wildcard pattern matching Supports "*" at the end (e.g., "/api/v1/auth/*" matches
   * "/api/v1/auth/login")
   */
  private boolean pathMatches(String pattern, String path) {
    if (pattern.endsWith("*")) {
      String prefix = pattern.substring(0, pattern.length() - 1);
      return path.startsWith(prefix);
    }
    return pattern.equals(path);
  }
}

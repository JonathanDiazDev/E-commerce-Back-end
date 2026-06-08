package com.jonathan.ecommerce.ratelimiting.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for enabling rate limiting on methods.
 *
 * <p>Three protection layers: 1. Endpoint-level: Global limit per endpoint 2. User-level:
 * Per-authenticated-user limit (optional) 3. IP-level: Per-source-IP limit
 *
 * <p>Example: @RateLimited( endpointKey = "login", endpointCapacity = 5, endpointRefillMinutes = 1,
 * userCapacity = 100, userRefillMinutes = 1, ipCapacity = 500, ipRefillMinutes = 5 ) public
 * ResponseEntity<?> login(@RequestBody LoginDTO dto) { ... }
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimited {
  // ===== ENDPOINT LEVEL (global limit) =====
  String value() default "";

  boolean failOpen() default true;
}

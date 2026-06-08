package com.jonathan.ecommerce.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
public class RequestContextUtil {

  private RequestContextUtil() {
    throw new IllegalStateException("Utility class");
  }

  /**
   * Extract the client's IP address from the HTTP request.
   *
   * <p>Handles proxies and load balancers by checking headers in order: 1. X-Forwarded-For
   * (standard proxy header, can contain multiple IPs) 2. X-Real-IP (alternative proxy header) 3.
   * request.getRemoteAddr() (direct connection)
   *
   * @return Client IP address, or null if unable to extract
   */
  public static String getClientIp() {
    try {
      ServletRequestAttributes requestAttributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (requestAttributes == null) {
        log.debug("RequestContextHolder returned null - not in HTTP request context");
        return null;
      }

      var request = requestAttributes.getRequest();

      String xForwardedFor = request.getHeader("X-Forwarded-For");
      if (xForwardedFor != null && !xForwardedFor.isBlank()) {
        String clientIp = xForwardedFor.split(",")[0].trim();
        log.debug("Client IP extracted from X-Forwarded-For: {}", clientIp);
        return clientIp;
      }

      String xRealIp = request.getHeader("X-Real-IP");
      if (xRealIp != null && !xRealIp.isEmpty()) {
        log.debug("Client IP extracted from X-Real-IP: {}", xRealIp);
        return xRealIp;
      }

      String remoteAddr = request.getRemoteAddr();
      log.debug("Client IP extracted from Remote-Addr: {}", remoteAddr);
      return remoteAddr;
    } catch (Exception e) {
      log.debug("Exception while getting IP address", e);
      return null;
    }
  }

  /**
   * Extract the authenticated user ID from the security context.
   *
   * <p>Returns the username/email of the currently authenticated user. Returns null if user is not
   * authenticated.
   *
   * <p>Customization tip: If your UserDetails has a custom id field, you can extract it like:
   *
   * <pre>
   * Object principal = auth.getPrincipal();
   * if (principal instanceof CustomUserDetails) {
   *   return ((CustomUserDetails) principal).getId().toString();
   * }
   * </pre>
   *
   * @return Authenticated user ID (username/email), or null if not authenticated
   */
  public static String getUserId() {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();

      // Check if user is authenticated
      if (auth != null && auth.isAuthenticated()) {
        Object principal = auth.getPrincipal();

        // Filter out "anonymousUser" placeholder
        if (!"anonymousUser".equals(principal)) {
          String userId = auth.getName();
          log.debug("User ID extracted from SecurityContext: {}", userId);
          return userId;
        }
      }

      log.debug("No authenticated user in SecurityContext");
      return null;

    } catch (Exception e) {
      log.debug("Could not extract user ID from SecurityContext", e);
      return null;
    }
  }

  /**
   * Check if the current user is authenticated.
   *
   * @return true if user is authenticated and not anonymous, false otherwise
   */
  public static boolean isAuthenticated() {
    try {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      return auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal());
    } catch (Exception e) {
      log.debug("Could not check authentication status", e);
      return false;
    }
  }

  /**
   * Get the user agent string from the request.
   *
   * <p>Useful for additional tracking or analytics.
   *
   * @return User-Agent header value, or null if not present
   */
  public static String getUserAgent() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        return attributes.getRequest().getHeader("User-Agent");
      }
    } catch (Exception e) {
      log.debug("Could not extract User-Agent", e);
    }
    return null;
  }

  /**
   * Get the request method (GET, POST, etc).
   *
   * @return HTTP method, or null if unable to extract
   */
  public static String getRequestMethod() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        return attributes.getRequest().getMethod();
      }
    } catch (Exception e) {
      log.debug("Could not extract request method", e);
    }
    return null;
  }

  /**
   * Get the request URI.
   *
   * @return Request URI, or null if unable to extract
   */
  public static String getRequestUri() {
    try {
      ServletRequestAttributes attributes =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

      if (attributes != null) {
        return attributes.getRequest().getRequestURI();
      }
    } catch (Exception e) {
      log.debug("Could not extract request URI", e);
    }
    return null;
  }
}

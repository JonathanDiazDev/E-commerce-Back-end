package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.repository.TokenRepository;
import com.jonathan.ecommerce.service.JwtService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@RequiredArgsConstructor
@Component
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
  private final JwtService jwtService;
  private final UserDetailsService userDetailsService;
  private final TokenRepository tokenRepository;
  private final TokenBlacklistService tokenBlacklistService;

  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      return null;
    }
    return header.substring(7);
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws IOException, ServletException {

    final String jwt = extractBearerToken(request);

    if (jwt != null) {
      try {
        authenticate(jwt, request);
      } catch (Exception e) {
        logger.error("JWT Authentication failed: Security context could not be established");
      }
    }

    chain.doFilter(request, response);
  }

  private boolean isTokenActive(String jwt) {
    if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
      log.debug("JWT_BLACKLISTED - Token was revoked");
      return false;
    }

    boolean isActive =
        tokenRepository
            .findByToken(jwt)
            .map(
                t -> {
                  boolean valid = !t.isExpired() && !t.isRevoked();
                  if (!valid) {
                    log.debug("JWT_INACTIVE - expired={} revoked={}", t.isExpired(), t.isRevoked());
                  }
                  return valid;
                })
            .orElse(false);

    if (!isActive) {
      log.debug("JWT_NOT_FOUND - Token not in database");
    }
    return isActive;
  }

  private void authenticate(String jwt, HttpServletRequest request) {
    final String userEmail = jwtService.extractUsername(jwt);
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
      if (jwtService.isAccessTokenValid(jwt, userDetails) && isTokenActive(jwt)) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }
  }
}

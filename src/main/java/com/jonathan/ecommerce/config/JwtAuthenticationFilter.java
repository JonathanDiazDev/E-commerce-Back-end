package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
  private final TokenBlacklistService tokenBlacklistService;

  private String extractBearerToken(HttpServletRequest request) {
    String header = request.getHeader("Authorization");
    log.info("Authorization header: '{}'", header);
    if (header == null || !header.startsWith("Bearer ")) {
      return null;
    }
    String token = header.substring(7);
    return token.isBlank() ? null : token;
  }

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws IOException, ServletException {

    log.info("Filter ejecutado para: {}", request.getRequestURI());
    String jwt = extractBearerToken(request);

    if (jwt == null) {
      jwt = extractCookieToken(request);
      log.info("JWT desde cookie: {}", jwt != null ? "OK" : "NULL");
    }

    if (jwt != null) {
      try {
        authenticate(jwt, request);
      } catch (Exception e) {
        logger.error("Invalid cookie token: {}", e);
        Cookie cookie = new Cookie("access_token", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        response.addCookie(cookie);
      }
    }
    chain.doFilter(request, response);
  }

  private boolean isTokenActive(String jwt) {
    // 1. Primero Redis (Es lo más rápido)
    if (tokenBlacklistService.isTokenBlacklisted(jwt)) {
      log.debug("Token en lista negra");
      return false;
    }
    return true;
  }

  private void authenticate(String jwt, HttpServletRequest request) {
    final String userEmail = jwtService.extractUsername(jwt);
    if (userEmail != null && SecurityContextHolder.getContext().getAuthentication() == null) {
      UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
      if (jwtService.isAccessTokenValid(jwt, userDetails) && isTokenActive(jwt)) {
        UsernamePasswordAuthenticationToken authToken =
            new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        log.info("Autoridades cargadas para el usuario: {}", userDetails.getAuthorities());
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

        SecurityContextHolder.getContext().setAuthentication(authToken);
      }
    }
  }

  private String extractCookieToken(HttpServletRequest request) {
    Cookie[] cookies = request.getCookies();
    if (cookies != null) {
      for (Cookie cookie : cookies) {
        if ("access_token".equals(cookie.getName())) {
          String val = cookie.getValue();
          // LOG CRÍTICO: Imprime el valor real para ver qué hay ahí
          log.info("DEBUG: Cookie name: {}, Value: '{}'", cookie.getName(), val);

          // Si el valor no tiene puntos, no es un JWT.
          if (val == null || !val.contains(".")) {
            log.warn("La cookie contiene un valor inválido que no parece JWT: {}", val);
            return null;
          }
          return val;
        }
      }
    }
    return null;
  }
}

package com.jonathan.ecommerce.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import lombok.NonNull;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class TraceIdFilter extends OncePerRequestFilter {
  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain filterChain)
      throws ServletException, IOException {
    String traceId = UUID.randomUUID().toString().substring(0, 10);

    MDC.put("traceId", traceId);
    response.setHeader("X-TraceId", traceId);

    try {
      filterChain.doFilter(request, response);
    } finally {
      MDC.clear();
    }
  }
}

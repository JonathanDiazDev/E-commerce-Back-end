package com.jonathan.ecommerce.config;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.impl.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
class JwtAuthenticationFilterTest {

  @Mock private JwtService jwtService;
  @Mock private UserDetailsService userDetailsService;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private FilterChain filterChain;

  @InjectMocks private JwtAuthenticationFilter jwtAuthenticationFilter;

  private MockHttpServletRequest request;
  private MockHttpServletResponse response;

  @BeforeEach
  void setUp() {
    request = new MockHttpServletRequest();
    response = new MockHttpServletResponse();
    SecurityContextHolder.clearContext();
  }

  @Test
  void doFilter_NoToken_ContinuesChain() throws Exception {
    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).extractUsername(anyString());
  }

  @Test
  void doFilter_ValidBearerToken_Authenticates() throws Exception {
    request.addHeader("Authorization", "Bearer valid.jwt.token");

    when(jwtService.extractUsername("valid.jwt.token")).thenReturn("test@test.com");
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
    when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
    when(jwtService.isAccessTokenValid("valid.jwt.token", userDetails)).thenReturn(true);
    when(tokenBlacklistService.isTokenBlacklisted("valid.jwt.token")).thenReturn(false);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername("valid.jwt.token");
  }

  @Test
  void doFilter_InvalidBearerToken_SkipsAuthentication() throws Exception {
    request.addHeader("Authorization", "Bearer invalid.token");

    when(jwtService.extractUsername("invalid.token")).thenReturn("test@test.com");
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
    when(jwtService.isAccessTokenValid("invalid.token", userDetails)).thenReturn(false);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService).extractUsername("invalid.token");
  }

  @Test
  void doFilter_BlacklistedToken_RemovesCookie() throws Exception {
    request.addHeader("Authorization", "Bearer blacklisted.jwt.token");

    when(jwtService.extractUsername("blacklisted.jwt.token")).thenReturn("test@test.com");
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
    when(jwtService.isAccessTokenValid("blacklisted.jwt.token", userDetails)).thenReturn(true);
    when(tokenBlacklistService.isTokenBlacklisted("blacklisted.jwt.token")).thenReturn(true);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_ValidCookieToken_Authenticates() throws Exception {
    request.setCookies(new Cookie("access_token", "valid.jwt.cookie"));

    when(jwtService.extractUsername("valid.jwt.cookie")).thenReturn("test@test.com");
    UserDetails userDetails = mock(UserDetails.class);
    when(userDetails.getAuthorities()).thenReturn(java.util.Collections.emptyList());
    when(userDetailsService.loadUserByUsername("test@test.com")).thenReturn(userDetails);
    when(jwtService.isAccessTokenValid("valid.jwt.cookie", userDetails)).thenReturn(true);
    when(tokenBlacklistService.isTokenBlacklisted("valid.jwt.cookie")).thenReturn(false);

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
  }

  @Test
  void doFilter_CookieWithoutDot_SkipsAuthentication() throws Exception {
    request.setCookies(new Cookie("access_token", "invalid-value-no-dots"));

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    verify(jwtService, never()).extractUsername(anyString());
  }

  @Test
  void doFilter_ExpiredTokenException_ClearsCookie() throws Exception {
    request.setCookies(new Cookie("access_token", "expired.jwt.token"));

    when(jwtService.extractUsername("expired.jwt.token"))
        .thenThrow(new RuntimeException("JWT expired"));

    jwtAuthenticationFilter.doFilterInternal(request, response, filterChain);

    verify(filterChain).doFilter(request, response);
    org.assertj.core.api.Assertions.assertThat(response.getCookies()).hasSize(1);
    org.assertj.core.api.Assertions.assertThat(response.getCookies()[0].getMaxAge()).isZero();
  }
}

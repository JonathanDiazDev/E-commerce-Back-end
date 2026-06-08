package com.jonathan.ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.Role;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.impl.AuthServiceImpl;
import com.jonathan.ecommerce.service.impl.JwtService;
import com.jonathan.ecommerce.util.HashUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetailsService;

@ExtendWith(MockitoExtension.class)
public class AuthServiceTest {
  @Mock private JwtService jwtService;

  @Mock private RefreshTokenRepository refreshTokenRepository;

  @InjectMocks private AuthServiceImpl authServiceImpl;

  @Mock private UserDetailsService userDetailsService;
  @Mock private UserRepository userRepository; // 👈 Agrega esto

  @Test
  void refreshToken_Success() {
    String plainToken = "token-real-123";
    String hashedToken = HashUtil.hashToken(plainToken);
    HttpServletResponse servletResponse = mock(HttpServletResponse.class);

    com.jonathan.ecommerce.entity.User userEntity = new com.jonathan.ecommerce.entity.User();
    userEntity.setEmail("usuario@test.com");
    userEntity.setRole(Role.USER);

    RefreshToken storedToken = new RefreshToken();
    storedToken.setRevoked(false);
    storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
    storedToken.setUser(userEntity);
    storedToken.setFamilyId(UUID.fromString(UUID.randomUUID().toString()));

    when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(storedToken));
    when(jwtService.isRefreshTokenValid(any(), any())).thenReturn(true);
    when(jwtService.generateToken(any())).thenReturn("new-access-token");
    when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");

    AuthUserResponse result = authServiceImpl.refreshToken(plainToken, servletResponse);

    assertNotNull(result);
    assertEquals("usuario@test.com", result.email());
    assertEquals("USER", result.role());
    assertTrue(storedToken.isRevoked());
  }

  @Test
  void refreshToken_ThrowsException_WhenTokenNotFound() {
    // 1. Arrange

    String plainToken = "token-que-no-existe";
    when(refreshTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

    // 2 & 3. Act & Assert (Se hacen juntos para excepciones)
    BadCredentialsException exception =
        assertThrows(
            BadCredentialsException.class,
            () -> {
              authServiceImpl.refreshToken(plainToken, mock(HttpServletResponse.class));
            });
    assertEquals("Invalid refresh token", exception.getMessage());
  }

  @Test
  void refreshToken_ThrowsException_WhenTokenExpired() {
    // 1. Arrange
    String plainToken = "token-expirado";
    String hashedToken = HashUtil.hashToken(plainToken);

    User userEntity = new User();
    userEntity.setEmail("test@test.com");
    userEntity.setRole(Role.USER);
    RefreshToken expiredToken = new RefreshToken();
    expiredToken.setRevoked(false);

    expiredToken.setUser(userEntity);
    expiredToken.setFamilyId(UUID.randomUUID());
    // Ponemos una fecha de hace 1 hora (ya expiró)
    expiredToken.setExpiresAt(Instant.now().minusSeconds(3600));

    when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(expiredToken));

    // 2 & 3. Act & Assert
    assertThrows(
        BadCredentialsException.class,
        () -> {
          authServiceImpl.refreshToken(plainToken, mock(HttpServletResponse.class));
        });
  }
}

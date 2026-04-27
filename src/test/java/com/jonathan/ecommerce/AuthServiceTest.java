package com.jonathan.ecommerce;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.jonathan.ecommerce.dto.response.AuthResponse;
import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.enums.Role;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.JwtService;
import com.jonathan.ecommerce.service.impl.AuthServiceImpl;
import com.jonathan.ecommerce.util.HashUtil;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UserDetails;
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
    String email = "usuario@test.com";
    String hashedToken = HashUtil.hashToken(plainToken);

    UserDetails userDetails =
        org.springframework.security.core.userdetails.User.withUsername(email)
            .password("password")
            .authorities("ROLE_USER")
            .build();

    RefreshToken storedToken = new RefreshToken();
    storedToken.setRevoked(false);
    storedToken.setExpiresAt(Instant.now().plusSeconds(3600));
    storedToken.setUser(
        new com.jonathan.ecommerce.entity.User()); // Asigna un usuario si es necesario

    // Definimos el comportamiento de los Mocks 🃏
    when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(storedToken));
    when(jwtService.extractUsername(plainToken)).thenReturn(email);
    when(userDetailsService.loadUserByUsername(email)).thenReturn(userDetails);
    when(jwtService.isRefreshTokenValid(any(), any())).thenReturn(true);


    // Creamos la entidad de usuario
    com.jonathan.ecommerce.entity.User userEntity = new com.jonathan.ecommerce.entity.User();
    userEntity.setEmail(email);
    userEntity.setRole(Role.USER);

// Entrenamos al nuevo mock
    when(userRepository.findByEmail(email)).thenReturn(Optional.of(userEntity));
    // Mocks para la generación de nuevos tokens
    when(jwtService.generateToken(any())).thenReturn("new-access-token");
    when(jwtService.generateRefreshToken(any())).thenReturn("new-refresh-token");

    // 2. Act (Actuar)
    AuthResponse response = authServiceImpl.refreshToken(plainToken);

    // 3. Assert (Verificar)
    assertNotNull(response);
    assertEquals("new-access-token", response.accessToken());
    assertTrue(storedToken.isRevoked()); // Verificamos que el token viejo SE REVOCÓ
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
              authServiceImpl.refreshToken(plainToken);
            });
    assertEquals("Token no encontrado", exception.getMessage());
  }

  @Test
  void refreshToken_ThrowsException_WhenTokenExpired() {
    // 1. Arrange
    String plainToken = "token-expirado";
    String hashedToken = HashUtil.hashToken(plainToken);

    RefreshToken expiredToken = new RefreshToken();
    expiredToken.setRevoked(false);
    // Ponemos una fecha de hace 1 hora (ya expiró)
    expiredToken.setExpiresAt(Instant.now().minusSeconds(3600));

    when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(expiredToken));

    // 2 & 3. Act & Assert
    assertThrows(BadCredentialsException.class, () -> {
      authServiceImpl.refreshToken(plainToken);
    });
  }
}

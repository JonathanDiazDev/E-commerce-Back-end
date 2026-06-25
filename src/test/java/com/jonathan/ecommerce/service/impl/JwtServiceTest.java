package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.Role;
import io.jsonwebtoken.JwtException;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class JwtServiceTest {

  private JwtService jwtService;
  private User user;
  private static final long ACCESS_TOKEN_EXPIRATION = 3600000L; // 1 hora

  @BeforeEach
  void setUp() {
    jwtService = new JwtService();
    ReflectionTestUtils.setField(
        jwtService,
        "secretKey",
        "aGVsbG8gdGhpcyBpcyBhIHZlcnkgc2VjdXJlIGtleSBmb3IgdGVzdGluZyBwdXJwb3Nlcw==");
    ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", ACCESS_TOKEN_EXPIRATION);
    ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);
    jwtService.validateSecretKey();

    user = new User();
    user.setEmail("test@test.com");
    user.setPassword("password");
    user.setRole(Role.USER);
  }

  @Test
  void generateToken_Success() {
    String token = jwtService.generateToken(user);
    assertThat(token).isNotNull().isNotEmpty();

    String username = jwtService.extractUsername(token);
    assertThat(username).isEqualTo("test@test.com");
  }

  @Test
  void generateRefreshToken_Success() {
    String token = jwtService.generateRefreshToken(user);
    assertThat(token).isNotNull().isNotEmpty();
  }

  @Test
  void isTokenValid_ValidToken_ReturnsTrue() {
    String token = jwtService.generateToken(user);
    assertThat(jwtService.isTokenValid(token, user)).isTrue();
  }

  @Test
  void isTokenValid_WrongUser_ReturnsFalse() {
    String token = jwtService.generateToken(user);
    User otherUser = new User();
    otherUser.setEmail("other@test.com");
    assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
  }

  @Test
  void isAccessTokenValid_AccessToken_ReturnsTrue() {
    String token = jwtService.generateToken(user);
    assertThat(jwtService.isAccessTokenValid(token, user)).isTrue();
  }

  @Test
  void isAccessTokenValid_RefreshToken_ReturnsFalse() {
    String token = jwtService.generateRefreshToken(user);
    assertThat(jwtService.isAccessTokenValid(token, user)).isFalse();
  }

  @Test
  void isRefreshTokenValid_RefreshToken_ReturnsTrue() {
    String token = jwtService.generateRefreshToken(user);
    assertThat(jwtService.isRefreshTokenValid(token, user)).isTrue();
  }

  @Test
  void isRefreshTokenValid_AccessToken_ReturnsFalse() {
    String token = jwtService.generateToken(user);
    assertThat(jwtService.isRefreshTokenValid(token, user)).isFalse();
  }

  @Test
  void extractUsername_Success() {
    String token = jwtService.generateToken(user);
    assertThat(jwtService.extractUsername(token)).isEqualTo("test@test.com");
  }

  @Test
  void extractExpiration_Success() {
    String token = jwtService.generateToken(user);
    Date expiration = jwtService.extractExpiration(token);

    // Calcula la fecha exacta esperada (Fecha actual + 1 hora de expiración)
    Date expectedExpiration = new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION);

    // Verificación estricta: Comprueba que la expiración sea coherente con un margen de 5 segundos
    assertThat(expiration).isCloseTo(expectedExpiration, 5000L);
  }

  @Test
  void extractClaim_Success() {
    String token = jwtService.generateToken(user);
    String tokenId = jwtService.extractTokenId(token);
    assertThat(tokenId).isNotNull().isNotEmpty();
  }

  @Test
  void isTokenValid_MalformedToken_ThrowsException() { // Nombre corregido acorde a la acción
    assertThrows(JwtException.class, () -> jwtService.isTokenValid("invalid.token.here", user));
  }

  @Test
  void validateSecretKey_TooShort_ThrowsException() {
    JwtService badService = new JwtService();
    ReflectionTestUtils.setField(badService, "secretKey", "short");
    assertThrows(IllegalStateException.class, badService::validateSecretKey);
  }
}

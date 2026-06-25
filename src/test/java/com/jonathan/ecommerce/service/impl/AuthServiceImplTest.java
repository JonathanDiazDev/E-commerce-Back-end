package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.ChangePasswordRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.dto.response.SessionResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.Role;
import com.jonathan.ecommerce.exception.EmailAlreadyExistsException;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import com.jonathan.ecommerce.util.HashUtil; // Importación corregida
import jakarta.servlet.http.HttpServletResponse;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

  @Mock private UserRepository userRepository;
  @Mock private PasswordEncoder passwordEncoder;
  @Mock private AuthenticationManager authenticationManager;
  @Mock private JwtService jwtService;
  @Mock private RefreshTokenRepository refreshTokenRepository;
  @Mock private TokenBlacklistService tokenBlacklistService;
  @Mock private SecurityHelper securityHelper;
  @Mock private ObjectMapper objectMapper;
  @Mock private OutboxRepository outboxRepository;
  @Mock private HttpServletResponse response; // Mock globalizado para evitar duplicación

  @InjectMocks private AuthServiceImpl authService;

  private User user;

  @BeforeEach
  void setUp() {
    user = new User();
    user.setId(1L);
    user.setEmail("test@test.com");
    user.setName("Test User");
    user.setPassword("encodedPassword");
    user.setRole(Role.USER);
  }

  @Test
  void register_Success() throws Exception {
    UserRequest request = new UserRequest("Test User", "test@test.com", "password123");

    when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
    when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");
    when(userRepository.saveAndFlush(any(User.class))).thenReturn(user);

    UserResponse result = authService.register(request);

    assertThat(result.email()).isEqualTo("test@test.com");
    assertThat(result.name()).isEqualTo("Test User");
    verify(outboxRepository).save(any(OutboxEvent.class));
  }

  @Test
  void register_EmailAlreadyExists() {
    UserRequest request = new UserRequest("Test", "test@test.com", "password");
    when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

    assertThrows(EmailAlreadyExistsException.class, () -> authService.register(request));
    verify(userRepository, never()).saveAndFlush(any());
  }

  @Test
  void login_Success() {
    AuthRequest authRequest = new AuthRequest("test@test.com", "password");

    Authentication authentication = mock(Authentication.class);
    when(authentication.getPrincipal()).thenReturn(user);
    when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
        .thenReturn(authentication);
    when(jwtService.generateToken(user)).thenReturn("access-token");
    when(jwtService.generateRefreshToken(user)).thenReturn("refresh-token");

    AuthUserResponse result = authService.login(authRequest, response);

    assertThat(result.email()).isEqualTo("test@test.com");
    assertThat(result.role()).isEqualTo("USER");
    verify(refreshTokenRepository).save(any(RefreshToken.class));
    verify(response, times(2)).addHeader(eq("Set-Cookie"), anyString());
  }

  @Test
  void login_InvalidCredentials() {
    AuthRequest authRequest = new AuthRequest("test@test.com", "wrong");

    when(authenticationManager.authenticate(any()))
        .thenThrow(new BadCredentialsException("Bad credentials"));

    assertThrows(BadCredentialsException.class, () -> authService.login(authRequest, response));
  }

  @Test
  void logout_Success() {
    when(jwtService.extractExpiration("access-token"))
        .thenReturn(java.util.Date.from(Instant.now().plusSeconds(3600)));

    authService.logout("refresh-token", "access-token", response);

    verify(tokenBlacklistService).blacklistToken(eq("access-token"), anyLong());
    verify(refreshTokenRepository).findByTokenHash(anyString());
    verify(response, times(2)).addHeader(eq("Set-Cookie"), anyString());
  }

  @Test
  void logoutAll_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(user);

    authService.logoutAll();

    verify(refreshTokenRepository).revokeAllByUserId(1L);
  }

  @Test
  void changePassword_Success() {
    ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass123");

    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);
    when(passwordEncoder.matches("newPass123", "encodedPassword")).thenReturn(false);
    when(passwordEncoder.encode("newPass123")).thenReturn("newEncoded");

    authService.changePassword(request, response);

    verify(userRepository).save(user);
    verify(refreshTokenRepository).revokeAllByUserId(1L);
    verify(response, times(2)).addHeader(eq("Set-Cookie"), anyString());
  }

  @Test
  void changePassword_WrongCurrentPassword() {
    ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass");

    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(passwordEncoder.matches("wrongPass", "encodedPassword")).thenReturn(false);

    assertThrows(
        BadCredentialsException.class, () -> authService.changePassword(request, response));
  }

  @Test
  void changePassword_SameAsOld() {
    ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "oldPass");

    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(passwordEncoder.matches("oldPass", "encodedPassword")).thenReturn(true);

    assertThrows(
        BadCredentialsException.class, () -> authService.changePassword(request, response));
  }

  @Test
  void me_Success() {
    when(securityHelper.getCurrentUser()).thenReturn(user);

    AuthUserResponse result = authService.me();

    assertThat(result.email()).isEqualTo("test@test.com");
    assertThat(result.name()).isEqualTo("Test User");
    assertThat(result.role()).isEqualTo("USER");
  }

  @Test
  void getActiveSessions_Success() {
    RefreshToken token = new RefreshToken();
    token.setFamilyId(UUID.randomUUID());
    token.setCreatedAt(Instant.now());
    token.setExpiresAt(Instant.now().plusSeconds(3600));
    token.setUserAgent("Mozilla");
    token.setIpAddress("127.0.0.1");

    when(securityHelper.getCurrentUser()).thenReturn(user);
    when(refreshTokenRepository.findAllByUserIdAndRevokedFalse(1L)).thenReturn(List.of(token));

    List<SessionResponse> result = authService.getActiveSessions();

    assertThat(result).hasSize(1);
    assertThat(result.get(0).familyId()).isEqualTo(token.getFamilyId());
    assertThat(result.get(0).userAgent()).isEqualTo("Mozilla");
    assertThat(result.get(0).ipAddress()).isEqualTo("127.0.0.1");
  }

  @Test
  void refreshToken_InvalidToken_ReturnsBadCredentials() {
    assertThrows(BadCredentialsException.class, () -> authService.refreshToken(null, response));
    assertThrows(BadCredentialsException.class, () -> authService.refreshToken("", response));
  }

  @Test
  void refreshToken_ReuseDetected() {
    String plainToken = "reused-token";
    String hashedToken = HashUtil.hashToken(plainToken);

    RefreshToken storedToken = new RefreshToken();
    storedToken.setRevoked(true);
    storedToken.setFamilyId(UUID.randomUUID());
    storedToken.setUser(user);

    when(refreshTokenRepository.findByTokenHash(hashedToken)).thenReturn(Optional.of(storedToken));

    assertThrows(
        BadCredentialsException.class, () -> authService.refreshToken(plainToken, response));

    verify(refreshTokenRepository).revokeAllByFamilyId(storedToken.getFamilyId());
    // Garantiza que no se emitan nuevos tokens por seguridad tras detectar una reutilización
    verifyNoInteractions(jwtService);
  }
}

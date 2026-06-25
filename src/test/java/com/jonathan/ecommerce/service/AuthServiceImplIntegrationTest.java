package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.config.BaseIntegrationTest;
import com.jonathan.ecommerce.dto.request.*;
import com.jonathan.ecommerce.dto.response.*;
import com.jonathan.ecommerce.entity.*;
import com.jonathan.ecommerce.entity.enums.*;
import com.jonathan.ecommerce.exception.EmailAlreadyExistsException;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.*;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import com.jonathan.ecommerce.service.impl.JwtService;
import com.jonathan.ecommerce.util.HashUtil;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuthServiceImplIntegrationTest extends BaseIntegrationTest {

  @Autowired private AuthService authService;
  @Autowired private UserRepository userRepository;
  @Autowired private RefreshTokenRepository refreshTokenRepository;
  @Autowired private OutboxRepository outboxRepository;
  @Autowired private PasswordEncoder passwordEncoder;
  @Autowired private JwtService jwtService;

  @MockitoBean private SecurityHelper securityHelper;
  @MockitoBean private OutboxProcessor outboxProcessor;
  @MockitoBean private ProxyManager<byte[]> proxyManager;
  @MockitoBean private StockNotificationService stockNotificationService;

  private User testUser;
  private static final String TEST_PASSWORD = "SecurePass123!";

  @BeforeEach
  void setUp() {
    testUser = new User();
    testUser.setName("Auth Test User");
    testUser.setEmail("auth-it-" + System.nanoTime() + "@test.com");
    testUser.setPassword(passwordEncoder.encode(TEST_PASSWORD));
    testUser.setRole(Role.USER);
    testUser = userRepository.save(testUser);
  }

  @Test
  void register_ShouldCreateUserAndOutboxEvent() {
    String email = "new-register-" + System.nanoTime() + "@test.com";
    UserRequest request = new UserRequest("New User", email, "MyPassword123!");

    UserResponse response = authService.register(request);

    assertThat(response).isNotNull();
    assertThat(response.email()).isEqualTo(email);
    assertThat(response.name()).isEqualTo("New User");
    assertThat(response.role()).isEqualTo("USER");

    User saved = userRepository.findByEmail(email).orElse(null);
    assertThat(saved).isNotNull();
    assertThat(passwordEncoder.matches("MyPassword123!", saved.getPassword())).isTrue();

    List<OutboxEvent> events = outboxRepository.findAll();
    assertThat(events).isNotEmpty();
  }

  @Test
  void register_ShouldThrowException_WhenDuplicateEmail() {
    UserRequest request = new UserRequest("Dup", testUser.getEmail(), "OtherPass123!");

    assertThatThrownBy(() -> authService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class);
  }

  @Test
  void login_ShouldReturnTokens_WhenCredentialsAreValid() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    AuthRequest request = new AuthRequest(testUser.getEmail(), TEST_PASSWORD);

    AuthUserResponse authResponse = authService.login(request, response);

    assertThat(authResponse).isNotNull();
    assertThat(authResponse.email()).isEqualTo(testUser.getEmail());
    assertThat(authResponse.role()).isEqualTo("USER");

    verify(response, atLeastOnce()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
  }

  @Test
  void login_ShouldThrowException_WhenBadCredentials() {
    HttpServletResponse response = mock(HttpServletResponse.class);
    AuthRequest request = new AuthRequest(testUser.getEmail(), "WrongPassword1!");

    assertThatThrownBy(() -> authService.login(request, response))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void logout_ShouldRevokeRefreshToken() {
    String accessToken = jwtService.generateToken(testUser);
    String refreshTokenStr = jwtService.generateRefreshToken(testUser);

    RefreshToken rt = new RefreshToken();
    rt.setTokenHash(HashUtil.hashToken(refreshTokenStr));
    rt.setUser(testUser);
    rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
    rt.setRevoked(false);
    rt.setFamilyId(UUID.randomUUID());
    refreshTokenRepository.save(rt);

    HttpServletResponse response = mock(HttpServletResponse.class);

    authService.logout(refreshTokenStr, accessToken, response);

    RefreshToken saved = refreshTokenRepository.findById(rt.getId()).orElseThrow();
    assertThat(saved.isRevoked()).isTrue();

    verify(response, atLeastOnce()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
  }

  @Test
  void me_ShouldReturnCurrentUser() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    AuthUserResponse response = authService.me();

    assertThat(response).isNotNull();
    assertThat(response.email()).isEqualTo(testUser.getEmail());
    assertThat(response.name()).isEqualTo(testUser.getName());
    assertThat(response.role()).isEqualTo("USER");
  }

  @Test
  void changePassword_ShouldUpdatePassword_WhenCurrentPasswordIsCorrect() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ChangePasswordRequest request = new ChangePasswordRequest(TEST_PASSWORD, "NewPass12345!");

    authService.changePassword(request, response);

    User updated = userRepository.findById(testUser.getId()).orElseThrow();
    assertThat(passwordEncoder.matches("NewPass12345!", updated.getPassword())).isTrue();
    assertThat(passwordEncoder.matches(TEST_PASSWORD, updated.getPassword())).isFalse();

    verify(response, times(2)).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
  }

  @Test
  void changePassword_ShouldThrowException_WhenCurrentPasswordIsWrong() {
    when(securityHelper.getCurrentUser()).thenReturn(testUser);
    HttpServletResponse response = mock(HttpServletResponse.class);
    ChangePasswordRequest request = new ChangePasswordRequest("WrongPass123!", "NewPass12345!");

    assertThatThrownBy(() -> authService.changePassword(request, response))
        .isInstanceOf(BadCredentialsException.class);
  }

  @Test
  void refreshToken_ShouldRotateTokens_WhenTokenIsValid() {
    String refreshTokenStr = jwtService.generateRefreshToken(testUser);

    RefreshToken rt = new RefreshToken();
    rt.setTokenHash(HashUtil.hashToken(refreshTokenStr));
    rt.setUser(testUser);
    rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
    rt.setRevoked(false);
    rt.setFamilyId(UUID.randomUUID());
    refreshTokenRepository.save(rt);

    HttpServletResponse response = mock(HttpServletResponse.class);

    AuthUserResponse result = authService.refreshToken(refreshTokenStr, response);

    assertThat(result).isNotNull();
    assertThat(result.email()).isEqualTo(testUser.getEmail());

    RefreshToken oldToken = refreshTokenRepository.findById(rt.getId()).orElseThrow();
    assertThat(oldToken.isRevoked()).isTrue();
    assertThat(oldToken.getReplacedByTokenHash()).isNotBlank();

    verify(response, atLeastOnce()).addHeader(eq(HttpHeaders.SET_COOKIE), anyString());
  }

  @Test
  void refreshToken_ShouldThrowException_WhenTokenIsRevoked() {
    String refreshTokenStr = jwtService.generateRefreshToken(testUser);

    RefreshToken rt = new RefreshToken();
    rt.setTokenHash(HashUtil.hashToken(refreshTokenStr));
    rt.setUser(testUser);
    rt.setExpiresAt(Instant.now().plus(Duration.ofDays(7)));
    rt.setRevoked(true);
    rt.setFamilyId(UUID.randomUUID());
    refreshTokenRepository.save(rt);

    HttpServletResponse response = mock(HttpServletResponse.class);

    assertThatThrownBy(() -> authService.refreshToken(refreshTokenStr, response))
        .isInstanceOf(BadCredentialsException.class)
        .hasMessageContaining("reuse detected");
  }

  @Test
  void logoutAll_ShouldRevokeAllUserTokens() {
    RefreshToken rt1 = new RefreshToken();
    rt1.setTokenHash("hash1-" + UUID.randomUUID());
    rt1.setUser(testUser);
    rt1.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
    rt1.setRevoked(false);
    rt1.setFamilyId(UUID.randomUUID());
    refreshTokenRepository.save(rt1);

    RefreshToken rt2 = new RefreshToken();
    rt2.setTokenHash("hash2-" + UUID.randomUUID());
    rt2.setUser(testUser);
    rt2.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
    rt2.setRevoked(false);
    rt2.setFamilyId(UUID.randomUUID());
    refreshTokenRepository.save(rt2);

    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    authService.logoutAll();

    List<RefreshToken> activeTokens =
        refreshTokenRepository.findAllByUserIdAndRevokedFalse(testUser.getId());
    assertThat(activeTokens).isEmpty();
  }

  @Test
  void getActiveSessions_ShouldReturnNonRevokedTokens() {
    RefreshToken rt = new RefreshToken();
    rt.setTokenHash("session-hash-" + UUID.randomUUID());
    rt.setUser(testUser);
    rt.setExpiresAt(Instant.now().plus(Duration.ofDays(1)));
    rt.setRevoked(false);
    rt.setFamilyId(UUID.randomUUID());
    rt.setUserAgent("TestAgent");
    rt.setIpAddress("127.0.0.1");
    refreshTokenRepository.save(rt);

    when(securityHelper.getCurrentUser()).thenReturn(testUser);

    List<SessionResponse> sessions = authService.getActiveSessions();

    assertThat(sessions).hasSize(1);
    assertThat(sessions.get(0).familyId()).isEqualTo(rt.getFamilyId());
    assertThat(sessions.get(0).userAgent()).isEqualTo("TestAgent");
    assertThat(sessions.get(0).ipAddress()).isEqualTo("127.0.0.1");
  }
}

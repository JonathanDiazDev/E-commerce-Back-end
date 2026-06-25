package com.jonathan.ecommerce.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.dto.event.UserRegisteredEvent;
import com.jonathan.ecommerce.dto.request.AuthRequest;
import com.jonathan.ecommerce.dto.request.ChangePasswordRequest;
import com.jonathan.ecommerce.dto.request.UserRequest;
import com.jonathan.ecommerce.dto.response.AuthUserResponse;
import com.jonathan.ecommerce.dto.response.SessionResponse;
import com.jonathan.ecommerce.dto.response.UserResponse;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.RefreshToken;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.entity.enums.Role;
import com.jonathan.ecommerce.exception.EmailAlreadyExistsException;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.AuthService;
import com.jonathan.ecommerce.service.TokenBlacklistService;
import com.jonathan.ecommerce.service.helper.SecurityHelper;
import com.jonathan.ecommerce.util.HashUtil;
import com.jonathan.ecommerce.util.RequestContextUtil;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

  public static final String ACCESS_TOKEN = "access_token";
  public static final String REFRESH_TOKEN = "refresh_token";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;
  private final RefreshTokenRepository refreshTokenRepository;
  private final TokenBlacklistService tokenBlacklistService;
  private final SecurityHelper securityHelper;
  private final ObjectMapper objectMapper;
  private final OutboxRepository outboxRepository;

  @Value("${security.jwt.refresh-token-expiration}")
  private long refreshTokenExpiration;

  @Value("${cookie.secure:false}")
  private boolean cookieSecure;

  @Value("${cookie.same-site:Strict}")
  private String cookieSameSite;

  @Override
  @Transactional
  public UserResponse register(UserRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException("The email is already registered");
    }
    User user = new User();
    user.setName(request.name());
    user.setEmail(request.email());
    user.setRole(Role.USER);
    user.setPassword(passwordEncoder.encode(request.password()));
    String mesageId = UUID.randomUUID().toString();

    UserRegisteredEvent userEvent =
        new UserRegisteredEvent(user.getEmail(), user.getName(), Instant.now(), mesageId);
    String payload;
    try {
      payload = objectMapper.writeValueAsString(userEvent);
      OutboxEvent event = new OutboxEvent();
      event.setEventType(EventType.USER_REGISTERED);
      event.setPayload(payload);
      event.setCreatedAt(Instant.now());
      event.setAggregateId(UUID.randomUUID().toString());
      event.setOutboxStatus(OutboxStatus.PENDING);
      event.setAggregateType(AggregateType.USER);
      userRepository.saveAndFlush(user);
      outboxRepository.save(event);
    } catch (DataIntegrityViolationException ex) {
      throw new EmailAlreadyExistsException("Email already registered");
    } catch (JsonProcessingException ex) {
      throw new RuntimeException("Error serializing user event", ex);
    }

    log.info("event=USER_REGISTER email={}", user.getEmail());
    return new UserResponse(
        user.getId(), user.getEmail(), user.getName(), user.getRole().name(), user.getCreatedAt());
  }

  @Override
  @Transactional
  public AuthUserResponse login(AuthRequest request, HttpServletResponse response) {
    log.info("event=LOGIN_ATTEMPT email:{}", request.email());

    Authentication authentication =
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.password()));

    User user = (User) authentication.getPrincipal();
    String accessToken = jwtService.generateToken(user);
    String refreshToken = jwtService.generateRefreshToken(user);

    UUID familyId = UUID.randomUUID();

    String userAgent = RequestContextUtil.getUserAgent();
    String ipAddress = RequestContextUtil.getClientIp();

    saveRefreshToken(user, refreshToken, familyId, userAgent, ipAddress);

    response.addHeader(
        HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN, accessToken, 3600L).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(REFRESH_TOKEN, refreshToken, 60L * 60L * 24L * 7L).toString());

    log.info("event=LOGIN_SUCCESS email={}", user.getEmail());

    return new AuthUserResponse(user.getEmail(), user.getUsername(), user.getRole().name());
  }

  @Override
  @Transactional
  public AuthUserResponse refreshToken(String refreshToken, HttpServletResponse response) {
    log.info("event=TOKEN_REFRESH_REQUESTED");

    if (refreshToken == null || refreshToken.isEmpty()) {
      throw new BadCredentialsException("Invalid refresh token");
    }

    String hashedToken = HashUtil.hashToken(refreshToken);

    RefreshToken refreshTokenEntity =
        refreshTokenRepository
            .findByTokenHash(hashedToken)
            .orElseThrow(
                () -> {
                  log.warn("event=REFRESH_TOKEN_NOT_FOUND");
                  return new BadCredentialsException("Invalid refresh token");
                });

    log.info("event=REFRESH_TOKEN_FOUND familyId={}", refreshTokenEntity.getFamilyId());

    if (refreshTokenEntity.isRevoked()) {
      log.warn(
          "event=REFRESH_TOKEN_REUSE_DETECTED familyId={} userId={}",
          refreshTokenEntity.getFamilyId(),
          refreshTokenEntity.getUser().getId());

      refreshTokenRepository.revokeAllByFamilyId(refreshTokenEntity.getFamilyId());

      log.warn("event=TOKEN_FAMILY_REVOKED familyId={}", refreshTokenEntity.getFamilyId());

      throw new BadCredentialsException("Refresh token reuse detected");
    }

    if (refreshTokenEntity.getExpiresAt().isBefore(Instant.now())) {

      log.warn(
          "event=REFRESH_TOKEN_EXPIRED familyId={} userId={}",
          refreshTokenEntity.getFamilyId(),
          refreshTokenEntity.getUser().getId());

      throw new BadCredentialsException("Refresh token expired");
    }

    User user = refreshTokenEntity.getUser();

    if (!jwtService.isRefreshTokenValid(refreshToken, user)) {
      log.warn("event=INVALID_REFRESH_TOKEN email={}", user.getEmail());
      throw new BadCredentialsException("Token invalido");
    }

    String newAccessToken = jwtService.generateToken(user);
    String newRefreshToken = jwtService.generateRefreshToken(user);
    String userAgent = RequestContextUtil.getUserAgent();
    String ipAddress = RequestContextUtil.getClientIp();

    refreshTokenEntity.setRevoked(true);
    refreshTokenEntity.setReplacedByTokenHash(HashUtil.hashToken(newRefreshToken));

    refreshTokenRepository.save(refreshTokenEntity);

    saveRefreshToken(user, newRefreshToken, refreshTokenEntity.getFamilyId(), userAgent, ipAddress);

    response.addHeader(
        HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN, newAccessToken, 3600L).toString());
    response.addHeader(
        HttpHeaders.SET_COOKIE,
        buildCookie(REFRESH_TOKEN, newRefreshToken, 60L * 60L * 24L * 7L).toString());

    log.info(
        "event=TOKEN_ROTATION_SUCCESS email={} familyId={}",
        user.getEmail(),
        refreshTokenEntity.getFamilyId());

    log.info("event=TOKEN_REFRESH_SUCCESS email={} role={}", user.getEmail(), user.getRole());
    return new AuthUserResponse(user.getEmail(), user.getName(), user.getRole().name());
  }

  @Override
  @Transactional
  public void logout(String refreshToken, String accessToken, HttpServletResponse response) {
    log.info("event=LOGOUT_REQUESTED");
    long expiration =
        jwtService.extractExpiration(accessToken).getTime() - System.currentTimeMillis();

    if (expiration > 0) {
      tokenBlacklistService.blacklistToken(accessToken, expiration);
      log.info("Access token blacklisted successfully. expirationMs={}", expiration);
    }

    String hashedToken = HashUtil.hashToken(refreshToken);

    refreshTokenRepository
        .findByTokenHash(hashedToken)
        .ifPresent(
            token -> {
              token.setRevoked(true);
              log.info("Refresh token revoked successfully. tokenHash={}", hashedToken);
            });
    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN, "", 0L).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN, "", 0L).toString());

    log.info("event=LOGOUT_SUCCESS");
  }

  @Override
  @Transactional
  public void logoutAll() {
    User user = securityHelper.getCurrentUser();
    log.info("event=LOGOUT_ALL_REQUESTED userEmail={}", user.getEmail());

    refreshTokenRepository.revokeAllByUserId(user.getId());
    log.info("event=LOGOUT_ALL_SUCCESS userEmail={} ", user.getEmail());
  }

  @Override
  @Transactional
  public void changePassword(ChangePasswordRequest request, HttpServletResponse response) {
    User user = securityHelper.getCurrentUser();
    log.info("event=CHANGE_PASSWORD_REQUEST userID={}", user.getId());

    if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
      log.warn("event=CHANGE_PASSWORD_FAILURE userEmail={} ", user.getEmail());
      throw new BadCredentialsException("Passwords don't match");
    }
    if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
      throw new BadCredentialsException("New password must be different from current password");
    }

    user.setPassword(passwordEncoder.encode(request.newPassword()));
    userRepository.save(user);

    refreshTokenRepository.revokeAllByUserId(user.getId());

    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(ACCESS_TOKEN, "", 0L).toString());
    response.addHeader(HttpHeaders.SET_COOKIE, buildCookie(REFRESH_TOKEN, "", 0L).toString());

    log.info("event=CHANGE_PASSWORD_SUCCESS userId={} ", user.getId());
  }

  @Override
  public AuthUserResponse me() {
    User user = securityHelper.getCurrentUser();
    return new AuthUserResponse(user.getEmail(), user.getName(), user.getRole().name());
  }

  @Override
  public List<SessionResponse> getActiveSessions() {
    User user = securityHelper.getCurrentUser();

    return refreshTokenRepository.findAllByUserIdAndRevokedFalse(user.getId()).stream()
        .map(
            refreshToken ->
                new SessionResponse(
                    refreshToken.getFamilyId(),
                    refreshToken.getCreatedAt(),
                    refreshToken.getExpiresAt(),
                    refreshToken.getUserAgent(),
                    refreshToken.getIpAddress()))
        .toList();
  }

  private void saveRefreshToken(
      User user, String refreshToken, UUID familyId, String userAgent, String ipAddress) {
    RefreshToken token = new RefreshToken();
    token.setUser(user);
    token.setTokenHash(HashUtil.hashToken(refreshToken));
    token.setExpiresAt(Instant.now().plusMillis(refreshTokenExpiration));
    token.setRevoked(false);
    token.setUserAgent(userAgent);
    token.setFamilyId(familyId);
    token.setIpAddress(ipAddress);
    refreshTokenRepository.save(token);
    log.info("event=REFRESH_TOKEN_SAVED userId={} familyId={}", user.getId(), familyId);
  }

  private ResponseCookie buildCookie(String name, String value, Long maxAgeSeconds) {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(cookieSecure)
        .path("/")
        .maxAge(Duration.ofSeconds(maxAgeSeconds))
        .sameSite(cookieSameSite)
        .build();
  }
}

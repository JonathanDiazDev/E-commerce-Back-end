package com.jonathan.ecommerce.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.dto.event.PasswordResetEvent;
import com.jonathan.ecommerce.dto.request.ForgotPasswordRequest;
import com.jonathan.ecommerce.dto.request.ResetPasswordRequest;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.PasswordResetToken;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.repository.PasswordResetTokenRepository;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.PasswordResetService;
import com.jonathan.ecommerce.util.HashUtil;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

  private final PasswordResetTokenRepository passwordResetTokenRepository;
  private final UserRepository userRepository;
  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;
  private final PasswordEncoder passwordEncoder;
  private final RefreshTokenRepository refreshTokenRepository;

  @Override
  @Transactional
  public String forgotPassword(ForgotPasswordRequest request) {
    userRepository
        .findByEmail(request.email())
        .ifPresent(
            user -> {
              String token = UUID.randomUUID().toString();
              String hashedToken = HashUtil.hashToken(token);
              String messageId = UUID.randomUUID().toString();

              PasswordResetToken resetToken = new PasswordResetToken();
              PasswordResetEvent event =
                  new PasswordResetEvent(request.email(), user.getName(), token, messageId);
              String payload;
              try {
                payload = objectMapper.writeValueAsString(event);
              } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
              }
              String finalPayload = payload;

              resetToken.setTokenHash(hashedToken);
              resetToken.setUser(user);
              resetToken.setExpiresAt(Instant.now().plus(Duration.ofMinutes(15)));
              OutboxEvent outboxEvent = new OutboxEvent();
              outboxEvent.setEventType(EventType.PASSWORD_RESET_REQUESTED);
              outboxEvent.setPayload(finalPayload);
              outboxEvent.setOutboxStatus(OutboxStatus.PENDING);
              outboxEvent.setAggregateId(token);
              outboxEvent.setAggregateType(AggregateType.USER);
              outboxEvent.setCreatedAt(Instant.now());

              outboxRepository.save(outboxEvent);
              passwordResetTokenRepository.save(resetToken);
            });

    return "If the account exists, a password reset email has been sent";
  }

  @Override
  public PasswordResetToken validateToken(String token) {
    if (token == null) {
      throw new IllegalArgumentException("Token cannot be null");
    }
    String hashedToken = HashUtil.hashToken(token);
    PasswordResetToken resetToken =
        passwordResetTokenRepository
            .findByTokenHash(hashedToken)
            .orElseThrow(() -> new ResourceNotFoundException("Token not found"));

    if (resetToken.getExpiresAt().isBefore(Instant.now())) {
      throw new IllegalArgumentException("Token is expired");
    }
    return resetToken;
  }

  @Override
  @Transactional
  public void resetPassword(ResetPasswordRequest request) {
    PasswordResetToken resetToken = validateToken(request.token());

    User user = resetToken.getUser();

    if (passwordEncoder.matches(request.newPassword(), user.getPassword())) {
      throw new BadCredentialsException("New password must be different from current password");
    }
    user.setPassword(passwordEncoder.encode(request.newPassword()));
    refreshTokenRepository.revokeAllByUserId(user.getId());
    passwordResetTokenRepository.delete(resetToken);

    userRepository.save(user);
  }
}

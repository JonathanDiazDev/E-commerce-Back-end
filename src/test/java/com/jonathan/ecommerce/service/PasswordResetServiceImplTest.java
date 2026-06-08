package com.jonathan.ecommerce.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.entity.PasswordResetToken;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.PasswordResetTokenRepository;
import com.jonathan.ecommerce.service.impl.PasswordResetServiceImpl;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceImplTest {

  @Mock private PasswordResetTokenRepository passwordResetTokenRepository;

  @InjectMocks private PasswordResetServiceImpl passwordResetServiceImpl;

  @Test
  public void validateToken_whenTokenIsNull_shouldThrowException() {
    assertThrows(
        IllegalArgumentException.class, () -> passwordResetServiceImpl.validateToken(null));
  }

  @Test
  public void validateToken_whenTokenNotFound_shouldThrowResourceNotFoundException() {
    when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());
    assertThrows(
        ResourceNotFoundException.class,
        () -> passwordResetServiceImpl.validateToken("some-invalid-token"));
  }

  @Test
  public void validateToken_whenTokenExpired() {
    PasswordResetToken token = new PasswordResetToken();
    token.setExpiresAt(Instant.now().minus(Duration.ofMinutes(30)));
    when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    assertThrows(
        IllegalArgumentException.class, () -> passwordResetServiceImpl.validateToken("some-token"));
  }

  @Test
  public void validateToken_whenTokenValid() {
    PasswordResetToken token = new PasswordResetToken();
    token.setTokenHash("some-token");
    token.setExpiresAt(Instant.now().plus(Duration.ofMinutes(30)));
    when(passwordResetTokenRepository.findByTokenHash(anyString())).thenReturn(Optional.of(token));

    PasswordResetToken result = passwordResetServiceImpl.validateToken("some-token");

    assertThat(result).isNotNull();
    assertThat(result.getTokenHash()).isEqualTo("some-token");
  }
}

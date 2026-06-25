package com.jonathan.ecommerce.kafka.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.PasswordResetEvent;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PasswordResetConsumerTest {

  @Mock private EmailService emailService;
  @Mock private FailedEmailRepository failedEmailRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private ProcessedEventRepository processedEventRepository;

  @InjectMocks private PasswordResetConsumer passwordResetConsumer;

  @Test
  void handlePasswordResetEvent_Success() throws MessagingException {
    ReflectionTestUtils.setField(passwordResetConsumer, "frontendUrl", "http://localhost:3000");

    PasswordResetEvent event =
        new PasswordResetEvent("test@test.com", "Test User", "reset-token-123", "msg-1");

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);

    passwordResetConsumer.handlePasswordResetEvent(event);

    verify(emailService)
        .sendResetPasswordEmail(eq("test@test.com"), eq("Test User"), contains("reset-token-123"));
    verify(processedEventRepository).save(any());
  }

  @Test
  void handlePasswordResetEvent_Duplicate() throws MessagingException {
    PasswordResetEvent event =
        new PasswordResetEvent("test@test.com", "Test User", "token", "msg-1");

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(true);

    passwordResetConsumer.handlePasswordResetEvent(event);

    verify(emailService, never()).sendResetPasswordEmail(anyString(), anyString(), anyString());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void handleDlt_Success() throws Exception {
    PasswordResetEvent event =
        new PasswordResetEvent("test@test.com", "Test User", "token", "msg-1");

    when(objectMapper.writeValueAsString(event)).thenReturn("{}");

    passwordResetConsumer.handleDlt(event, "Connection refused");

    verify(failedEmailRepository).save(any(FailedEmail.class));
  }

  @Test
  void handleDlt_SerializationError() throws Exception {
    PasswordResetEvent event =
        new PasswordResetEvent("test@test.com", "Test User", "token", "msg-1");

    when(objectMapper.writeValueAsString(event)).thenThrow(new RuntimeException("JSON error"));

    passwordResetConsumer.handleDlt(event, "Connection refused");

    verify(failedEmailRepository).save(any(FailedEmail.class));
  }
}

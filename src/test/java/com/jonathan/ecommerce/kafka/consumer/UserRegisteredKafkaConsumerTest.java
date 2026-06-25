package com.jonathan.ecommerce.kafka.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.UserRegisteredEvent;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserRegisteredKafkaConsumerTest {

  @Mock private EmailService emailService;
  @Mock private FailedEmailRepository failedEmailRepository;
  @Mock private ProcessedEventRepository processedEventRepository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private UserRegisteredKafkaConsumer userRegisteredKafkaConsumer;

  @Test
  void listenUserRegistered_Success() throws MessagingException {
    UserRegisteredEvent event =
        new UserRegisteredEvent("test@test.com", "Test User", Instant.now(), "msg-1");

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);

    userRegisteredKafkaConsumer.listenUserRegistered(event);

    verify(emailService).sendRegisterEmail("test@test.com", "Test User", event.createdAt());
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  @Test
  void listenUserRegistered_Duplicate() throws MessagingException {
    UserRegisteredEvent event =
        new UserRegisteredEvent("test@test.com", "Test User", Instant.now(), "msg-1");

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(true);

    userRegisteredKafkaConsumer.listenUserRegistered(event);

    verify(emailService, never()).sendRegisterEmail(anyString(), anyString(), any());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void handleDlt_Success() throws Exception {
    UserRegisteredEvent event =
        new UserRegisteredEvent("test@test.com", "Test User", Instant.now(), "msg-1");

    when(objectMapper.writeValueAsString(event)).thenReturn("{}");

    userRegisteredKafkaConsumer.handleDlt(event, "Email send failed");

    verify(failedEmailRepository).save(any(FailedEmail.class));
  }

  @Test
  void handleDlt_SerializationError() throws Exception {
    UserRegisteredEvent event =
        new UserRegisteredEvent("test@test.com", "Test User", Instant.now(), "msg-1");

    when(objectMapper.writeValueAsString(event)).thenThrow(new RuntimeException("JSON error"));

    userRegisteredKafkaConsumer.handleDlt(event, "Email send failed");

    verify(failedEmailRepository).save(any(FailedEmail.class));
  }
}

package com.jonathan.ecommerce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.PasswordResetEvent;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.entity.enums.EmailType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PasswordResetConsumer {
  private final EmailService emailService;
  private final FailedEmailRepository failedEmailRepository;
  private final ObjectMapper objectMapper;
  private final ProcessedEventRepository processedEventRepository;

  @Value("${app.frontend-url}")
  private String frontendUrl;

  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
  @KafkaListener(topics = KafkaConfig.PASSWORD_RESET_TOPIC, groupId = "email-service-group")
  public void handlePasswordResetEvent(PasswordResetEvent event) throws MessagingException {
    log.info("Processing password reset for {} ", event.email());

    if (!processedEventRepository.existsByIdemKey(event.messageId())) {
      ProcessedEvent processedEvent = new ProcessedEvent();
      processedEvent.setIdemKey(event.messageId());
      processedEvent.setEventType(EventType.PASSWORD_RESET_REQUESTED);
      processedEvent.setEmail(event.email());
      processedEvent.setProcessedAt(Instant.now());
      String resetLink = frontendUrl + "/reset-password?token=" + event.token();
      emailService.sendResetPasswordEmail(event.email(), event.userName(), resetLink);
      processedEventRepository.save(processedEvent);
      log.info("Password reset email sent to {} ", event.email());
    }
  }

  @DltHandler
  public void handleDlt(
      PasswordResetEvent event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {

    log.error("❌ Final failure after retries. Saving failed email for {}", event.email());

    String payload;

    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (Exception e) {
      log.error("Error serializing email payload", e);
      payload = "{\"error\":\"serialization_failed\"}";
    }

    FailedEmail failedEmail =
        FailedEmail.builder()
            .recipient(event.email())
            .emailType(EmailType.PASSWORD_RESET)
            .payload(payload)
            .errorMessage(errorMessage)
            .occurredAt(Instant.now())
            .retryCount(3) // ya agotó los 3 intentos de Kafka
            .status(EmailStatus.PENDING)
            .build();

    failedEmailRepository.save(failedEmail);

    log.info("Failed email stored for recipient {}", event.email());
  }
}

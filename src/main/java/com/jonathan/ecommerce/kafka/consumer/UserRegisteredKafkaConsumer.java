package com.jonathan.ecommerce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.UserRegisteredEvent;
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
public class UserRegisteredKafkaConsumer {

  private final EmailService emailService;
  private final FailedEmailRepository failedEmailRepository;
  private final ProcessedEventRepository processedEventRepository;
  private final ObjectMapper objectMapper;

  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
  @KafkaListener(topics = KafkaConfig.USER_REGISTERED_TOPIC, groupId = "user-registered-topic")
  public void listenUserRegistered(UserRegisteredEvent event) throws MessagingException {

    log.info("Received UserRegistered event for user {}", event.userName());
    if (!processedEventRepository.existsByIdemKey(event.messageId())) {
      ProcessedEvent processedEvent = new ProcessedEvent();
      processedEvent.setEventType(EventType.USER_REGISTERED);
      processedEvent.setIdemKey(event.messageId());
      processedEvent.setEmail(event.email());
      processedEvent.setProcessedAt(Instant.now());
      emailService.sendRegisterEmail(event.email(), event.userName(), event.createdAt());
      processedEventRepository.save(processedEvent);
      log.info("UserRegistered event sent to {}", event.email());
    }
  }

  @DltHandler
  public void handleDlt(
      UserRegisteredEvent event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
    log.error(
        "❌ Final failure after retries. Saving failed email for {} event of User registered email",
        event.email());

    String payload;
    try {
      payload = objectMapper.writeValueAsString(event);
    } catch (Exception e) {
      log.error("Error while converting UserRegisteredEvent to JSON", e);
      payload = "{\"error\":\"serialization_failed\"}";
    }

    FailedEmail failedEmail =
        FailedEmail.builder()
            .recipient(event.email())
            .emailType(EmailType.WELCOME)
            .payload(payload)
            .errorMessage(errorMessage)
            .occurredAt(Instant.now())
            .retryCount(3)
            .status(EmailStatus.PENDING)
            .build();

    failedEmailRepository.save(failedEmail);

    log.info("Failed email stored for recipient {}", event.email());
  }
}

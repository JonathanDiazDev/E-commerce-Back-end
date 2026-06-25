package com.jonathan.ecommerce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.EmailRequest;
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
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class EmailKafkaConsumer {

  private final EmailService emailService;
  private final FailedEmailRepository failedEmailRepository;
  private final ObjectMapper objectMapper;
  private final ProcessedEventRepository processedEventRepository;

  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
  @KafkaListener(topics = KafkaConfig.EMAIL_TOPIC, groupId = "email-service-group")
  public void listen(EmailRequest request) throws MessagingException {
    log.info("📥 Mensaje recibido en el consumidor: {}", request);
    log.info("Shipping attempt for: {}", request.to());
    Object stockObj = request.properties().get("totalStock");

    if (!(stockObj instanceof Number stockNumber)) {
      throw new IllegalArgumentException(
          "The stock cannot be zero for the product: " + request.productName());
    }

    Integer stock = stockNumber.intValue();

    if (!processedEventRepository.existsByIdemKey(request.messageId())) {

      ProcessedEvent processedEvent = new ProcessedEvent();
      processedEvent.setIdemKey(request.messageId());
      processedEvent.setEventType(EventType.RESTOCK_EMAIL);
      processedEvent.setEmail(request.to());
      processedEvent.setProcessedAt(Instant.now());

      emailService.sendStockAvailabilityEmail(
          request.to(), request.userName(), request.productName(), stock);
      processedEventRepository.save(processedEvent);
      log.info("✅ Email sent successfully!");
    }
  }

  @DltHandler
  public void handleDlt(
      EmailRequest request, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {

    log.error("❌ Final failure after retries. Saving failed email for {}", request.to());

    String payload;

    try {
      payload = objectMapper.writeValueAsString(request);
    } catch (Exception e) {
      log.error("Error serializing email payload", e);
      payload = "{\"error\":\"serialization_failed\"}";
    }

    FailedEmail failedEmail =
        FailedEmail.builder()
            .recipient(request.to())
            .emailType(EmailType.STOCK_AVAILABLE)
            .payload(payload)
            .errorMessage(errorMessage)
            .occurredAt(Instant.now())
            .retryCount(3) // ya agotó los 3 intentos de Kafka
            .status(EmailStatus.PENDING)
            .build();

    failedEmailRepository.save(failedEmail);

    log.info("Failed email stored for recipient {}", request.to());
  }
}

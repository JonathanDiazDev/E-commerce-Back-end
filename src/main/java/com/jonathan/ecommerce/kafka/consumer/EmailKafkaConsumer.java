package com.jonathan.ecommerce.kafka.consumer;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.service.EmailService;
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
public class EmailKafkaConsumer {

  private final EmailService emailService;
  private final FailedEmailRepository failedEmailRepository;

  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
  @KafkaListener(topics = KafkaConfig.EMAIL_TOPIC, groupId = "email-service-group")
  public void listen(EmailRequest request) {
    log.info("📥 Mensaje recibido en el consumidor: {}", request);
    log.info("Shipping attempt for: {}", request.to());
    Integer stock = (Integer) request.properties().get("totalStock");
    if (stock == null) {
      throw new IllegalArgumentException(
          "The stock cannot be zero for the product: " + request.productName());
    }
    emailService.sendStockAvailabilityEmail(request.to(), request.productName(), stock);
    log.info("✅ Email sent successfully!");
  }

  @DltHandler
  public void handleDlt(
      EmailRequest request, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String errorMessage) {
    log.error("❌ Final failure after retries. Saving to DB: {}", request.to());
    FailedEmail failedEmail =
        failedEmailRepository
            .findByRecipientAndProductName(request.to(), request.productName())
            .map(
                existing -> {
                  existing.setErrorMessage(errorMessage);
                  existing.setOccurredAt(Instant.now());
                  existing.setRetryCount(existing.getRetryCount() + 1);
                  return existing;
                })
            .orElseGet(
                () -> {
                  return FailedEmail.builder()
                      .recipient(request.to())
                      .productName(request.productName())
                      .errorMessage(errorMessage)
                      .occurredAt(Instant.now())
                      .retryCount(3)
                      .status(EmailStatus.PENDING)
                      .build();
                });
    failedEmailRepository.save(failedEmail);
  }
}

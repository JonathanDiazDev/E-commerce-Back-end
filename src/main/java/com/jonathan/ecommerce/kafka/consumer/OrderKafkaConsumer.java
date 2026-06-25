package com.jonathan.ecommerce.kafka.consumer;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaConsumer {

  private final EmailService emailService;
  private final ProcessedEventRepository processedEventRepository;

  @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
  @KafkaListener(topics = KafkaConfig.ORDER_EVENT, groupId = "email-service-group")
  public void consumeOrderEvent(OrderPlacedEvent event) throws MessagingException {

    log.info("Received order event {}", event);

    if (!processedEventRepository.existsByIdemKey(event.messageId())) {
      ProcessedEvent processedEvent = new ProcessedEvent();
      processedEvent.setIdemKey(event.messageId());
      processedEvent.setEventType(EventType.ORDER_PLACED);
      processedEvent.setEmail(event.userEmail());
      processedEvent.setProcessedAt(Instant.now());

      emailService.sendOrderConfirmationEmail(event);
      processedEventRepository.save(processedEvent);
      log.info("Processed Order Placed event {}", processedEvent);
    }
  }
}

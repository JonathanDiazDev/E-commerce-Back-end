package com.jonathan.ecommerce.kafka.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.event.PasswordResetEvent;
import com.jonathan.ecommerce.dto.event.UserRegisteredEvent;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.kafka.producer.OrderKafkaProducer;
import com.jonathan.ecommerce.kafka.producer.PasswordResetKafkaProducer;
import com.jonathan.ecommerce.kafka.producer.UserRegisteredKafkaProducer;
import com.jonathan.ecommerce.repository.OutboxRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class OutboxProcessor {
  private final OutboxRepository repository;
  private final OrderKafkaProducer producer;
  private final PasswordResetKafkaProducer resetProducer;
  private final ObjectMapper objectMapper;
  private final UserRegisteredKafkaProducer userRegisteredProducer;

  // Este método se ejecuta en su propia transacción para cada evento
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public void processSingleEvent(OutboxEvent outboxEvent) {

    try {

      switch (outboxEvent.getEventType()) {
        case EventType.ORDER_PLACED -> {
          OrderPlacedEvent event =
              objectMapper.readValue(outboxEvent.getPayload(), OrderPlacedEvent.class);

          producer.sendOrderOutboxEvents(event);
        }

        case EventType.PASSWORD_RESET_REQUESTED -> {
          PasswordResetEvent event =
              objectMapper.readValue(outboxEvent.getPayload(), PasswordResetEvent.class);

          resetProducer.sendPasswordResetEvent(event);
        }

        case EventType.USER_REGISTERED -> {
          UserRegisteredEvent event =
              objectMapper.readValue(outboxEvent.getPayload(), UserRegisteredEvent.class);
          userRegisteredProducer.sendEmailUserRegistered(event);
        }

        default ->
            throw new IllegalArgumentException(
                "Unsupported event type: " + outboxEvent.getEventType());
      }

      outboxEvent.setProcessedAt(Instant.now());
      outboxEvent.setOutboxStatus(OutboxStatus.SENT);

    } catch (Exception e) {

      outboxEvent.setOutboxStatus(OutboxStatus.FAILED);
    }

    repository.save(outboxEvent);
  }
}

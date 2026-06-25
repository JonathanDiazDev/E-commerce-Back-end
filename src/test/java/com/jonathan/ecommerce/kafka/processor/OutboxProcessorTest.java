package com.jonathan.ecommerce.kafka.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

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
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxProcessorTest {

  @Mock private OutboxRepository repository;
  @Mock private OrderKafkaProducer producer;
  @Mock private PasswordResetKafkaProducer resetProducer;
  @Mock private ObjectMapper objectMapper;
  @Mock private UserRegisteredKafkaProducer userRegisteredProducer;

  @InjectMocks private OutboxProcessor outboxProcessor;

  private OutboxEvent outboxEvent;

  @BeforeEach
  void setUp() {
    outboxEvent = new OutboxEvent();
    outboxEvent.setId(1L);
    outboxEvent.setPayload("{}");
    outboxEvent.setOutboxStatus(OutboxStatus.PENDING);
  }

  @Test
  void processSingleEvent_OrderPlaced() throws Exception {
    outboxEvent.setEventType(EventType.ORDER_PLACED);
    outboxEvent.setAggregateId("1");

    OrderPlacedEvent orderEvent =
        new OrderPlacedEvent(
            "test@test.com",
            "Test",
            1L,
            List.of("Product"),
            BigDecimal.valueOf(100),
            "msg-1",
            "pending",
            "3-5 days");

    when(objectMapper.readValue("{}", OrderPlacedEvent.class)).thenReturn(orderEvent);

    outboxProcessor.processSingleEvent(outboxEvent);

    verify(producer).sendOrderOutboxEvents(orderEvent);
    verify(repository).save(outboxEvent);
    assertThat(outboxEvent.getOutboxStatus()).isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void processSingleEvent_PasswordReset() throws Exception {
    outboxEvent.setEventType(EventType.PASSWORD_RESET_REQUESTED);
    outboxEvent.setAggregateId("1");

    PasswordResetEvent resetEvent =
        new PasswordResetEvent("test@test.com", "Test", "token", "msg-1");

    when(objectMapper.readValue("{}", PasswordResetEvent.class)).thenReturn(resetEvent);

    outboxProcessor.processSingleEvent(outboxEvent);

    verify(resetProducer).sendPasswordResetEvent(resetEvent);
    verify(repository).save(outboxEvent);
    assertThat(outboxEvent.getOutboxStatus()).isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void processSingleEvent_UserRegistered() throws Exception {
    outboxEvent.setEventType(EventType.USER_REGISTERED);
    outboxEvent.setAggregateId("1");

    UserRegisteredEvent userEvent =
        new UserRegisteredEvent("test@test.com", "Test", Instant.now(), "msg-1");

    when(objectMapper.readValue("{}", UserRegisteredEvent.class)).thenReturn(userEvent);

    outboxProcessor.processSingleEvent(outboxEvent);

    verify(userRegisteredProducer).sendEmailUserRegistered(userEvent);
    verify(repository).save(outboxEvent);
    assertThat(outboxEvent.getOutboxStatus()).isEqualTo(OutboxStatus.SENT);
  }

  @Test
  void processSingleEvent_UnsupportedEventType() {
    outboxEvent.setEventType(EventType.PAYMENT_RETRY);

    outboxProcessor.processSingleEvent(outboxEvent);

    verify(repository).save(outboxEvent);
    assertThat(outboxEvent.getOutboxStatus()).isEqualTo(OutboxStatus.FAILED);
  }

  @Test
  void processSingleEvent_DeserializationError() throws Exception {
    outboxEvent.setEventType(EventType.ORDER_PLACED);

    when(objectMapper.readValue("{}", OrderPlacedEvent.class))
        .thenThrow(new RuntimeException("JSON error"));

    outboxProcessor.processSingleEvent(outboxEvent);

    verify(producer, never()).sendOrderOutboxEvents(any());
    verify(repository).save(outboxEvent);
    assertThat(outboxEvent.getOutboxStatus()).isEqualTo(OutboxStatus.FAILED);
  }
}

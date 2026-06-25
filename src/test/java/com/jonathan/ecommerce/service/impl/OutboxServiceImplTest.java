package com.jonathan.ecommerce.service.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.repository.OutboxRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxServiceImplTest {

  @Mock private OutboxRepository outboxRepository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private OutboxServiceImpl outboxService;

  @Test
  void saveOutboxEvent_Success() throws Exception {
    OrderPlacedEvent event =
        new OrderPlacedEvent(
            "test@test.com",
            "Test",
            1L,
            List.of("Product"),
            BigDecimal.valueOf(100),
            "msg-1",
            "pending",
            "3-5 days");

    when(objectMapper.writeValueAsString(event)).thenReturn("{}");

    outboxService.saveOutboxEvent(AggregateType.ORDER, "1", EventType.ORDER_PLACED, event);

    verify(outboxRepository).save(any(OutboxEvent.class));
  }

  @Test
  void saveOutboxEvent_ThrowsException_WhenSerializationFails() throws Exception {
    when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("JSON error"));

    org.junit.jupiter.api.Assertions.assertThrows(
        Exception.class,
        () ->
            outboxService.saveOutboxEvent(
                AggregateType.ORDER, "1", EventType.ORDER_PLACED, new Object()));

    verify(outboxRepository, never()).save(any());
  }
}

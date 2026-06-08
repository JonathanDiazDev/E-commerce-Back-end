package com.jonathan.ecommerce.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.service.OutboxService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OutboxServiceImpl implements OutboxService {

  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void saveOutboxEvent(
      AggregateType aggregateType, String aggregateId, EventType eventType, Object event)
      throws Exception {

    String payload = objectMapper.writeValueAsString(event);

    OutboxEvent outboxEvent = new OutboxEvent();
    outboxEvent.setEventType(eventType);
    outboxEvent.setAggregateId(aggregateId);
    outboxEvent.setAggregateType(aggregateType);
    outboxEvent.setPayload(payload);
    outboxEvent.setOutboxStatus(OutboxStatus.PENDING);
    outboxEvent.setCreatedAt(Instant.now());

    outboxRepository.save(outboxEvent);
  }
}

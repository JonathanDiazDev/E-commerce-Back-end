package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.entity.enums.AggregateType;
import com.jonathan.ecommerce.entity.enums.EventType;

public interface OutboxService {
  void saveOutboxEvent(
      AggregateType aggregateType, String aggregateId, EventType eventType, Object event)
      throws Exception;
}

package com.jonathan.ecommerce.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.enums.OrderOutboxStatus;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.OrderOutboxEvent;
import com.jonathan.ecommerce.repository.OrderOutboxRepository;
import com.jonathan.ecommerce.service.OrderOutboxService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderOutboxServiceImpl implements OrderOutboxService {

  private final OrderOutboxRepository orderOutboxRepository;
  private final ObjectMapper objectMapper;

  @Override
  public void saveOutboxEvent(Order order, OrderPlacedEvent event) throws Exception{

    String payload = objectMapper.writeValueAsString(event);

    OrderOutboxEvent outboxEvent = new OrderOutboxEvent();
    outboxEvent.setOrder(order);
    outboxEvent.setPayload(payload);
    outboxEvent.setOrderOutboxStatus(OrderOutboxStatus.PENDING);
    outboxEvent.setCreatedAt(Instant.now());

    orderOutboxRepository.save(outboxEvent);
  }
}

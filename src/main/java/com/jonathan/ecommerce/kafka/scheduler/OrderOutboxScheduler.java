package com.jonathan.ecommerce.kafka.scheduler;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import com.jonathan.ecommerce.dto.enums.OrderOutboxStatus;
import com.jonathan.ecommerce.entity.OrderOutboxEvent;
import com.jonathan.ecommerce.kafka.producer.OrderKafkaProducer;
import com.jonathan.ecommerce.repository.OrderOutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderOutboxScheduler {

  private final OrderOutboxRepository orderOutboxRepository;
  private final ObjectMapper objectMapper;
  private final OrderKafkaProducer orderKafkaProducer;

  /**
   * The `volatile` flag caches if there were events in the previous loop. `volatile` ensures that
   * changes in one thread are immediately visible to other threads (multithreaded visibility).
   * Optimization: avoids database queries when there are no pending events. - If true: execute the
   * query - If false: skip the query (saves ~75% of unnecessary queries)
   */
  private volatile boolean hasEvent = true;

  @Transactional
  @Scheduled(fixedDelay = 10000)
  public void processOrderOutboxEvents() {
    if (hasEvent) {
      List<OrderOutboxEvent> pending =
          orderOutboxRepository.findByOrderOutboxStatus(OrderOutboxStatus.PENDING);

      if (pending.isEmpty()) {
        hasEvent = false;
        return;
      }

      for (OrderOutboxEvent outboxEvent : pending) {
        try {
          OrderPlacedEvent event =
              objectMapper.readValue(outboxEvent.getPayload(), OrderPlacedEvent.class);

          try {
            orderKafkaProducer.sendOrderOutboxEvents(event);
            outboxEvent.setOrderOutboxStatus(OrderOutboxStatus.SENT);
            orderOutboxRepository.save(outboxEvent);
          } catch (Exception e) {
            outboxEvent.setOrderOutboxStatus(OrderOutboxStatus.FAILED);
            orderOutboxRepository.save(outboxEvent);
            log.error("Kafka error for order: {}", outboxEvent.getOrder().getId(), e);
          }

        } catch (JsonProcessingException e) {
          outboxEvent.setOrderOutboxStatus(OrderOutboxStatus.FAILED);
          orderOutboxRepository.save(outboxEvent);
          log.error("JSON error for order: {}", outboxEvent.getOrder().getId(), e);
        }
      }
      log.info("Successfully processed {} outbox events", pending.size());
      hasEvent = false;
      return;
    }
    hasEvent = true;
  }
}

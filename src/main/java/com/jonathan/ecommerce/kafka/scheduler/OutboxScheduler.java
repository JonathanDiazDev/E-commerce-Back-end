package com.jonathan.ecommerce.kafka.scheduler;

import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.OutboxRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OutboxScheduler {

  private final OutboxRepository outboxRepository;
  private final OutboxProcessor outboxProcessor;

  @Scheduled(fixedDelay = 30000)
  public void processOutboxEvents() {
    List<OutboxEvent> pending =
        outboxRepository.findTop50ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.PENDING);

    if (pending.isEmpty()) {
      log.debug("No outbox events found");
      return;
    }

    for (OutboxEvent event : pending) {
      try {
        outboxProcessor.processSingleEvent(event);
      } catch (Exception ex) {
        log.error("Error processing outbox event {}", event.getId(), ex);
      }
    }
  }
}

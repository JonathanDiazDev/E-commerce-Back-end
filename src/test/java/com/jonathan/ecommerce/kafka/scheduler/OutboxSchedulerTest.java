package com.jonathan.ecommerce.kafka.scheduler;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.kafka.processor.OutboxProcessor;
import com.jonathan.ecommerce.repository.OutboxRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OutboxSchedulerTest {

  @Mock private OutboxRepository outboxRepository;
  @Mock private OutboxProcessor outboxProcessor;

  @InjectMocks private OutboxScheduler outboxScheduler;

  @Test
  void processOutboxEvents_Success() {
    OutboxEvent event1 = new OutboxEvent();
    event1.setId(1L);
    event1.setOutboxStatus(OutboxStatus.PENDING);

    OutboxEvent event2 = new OutboxEvent();
    event2.setId(2L);
    event2.setOutboxStatus(OutboxStatus.PENDING);

    when(outboxRepository.findTop50ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(event1, event2));

    outboxScheduler.processOutboxEvents();

    verify(outboxProcessor).processSingleEvent(event1);
    verify(outboxProcessor).processSingleEvent(event2);
  }

  @Test
  void processOutboxEvents_NoPendingEvents() {
    when(outboxRepository.findTop50ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of());

    outboxScheduler.processOutboxEvents();

    verify(outboxProcessor, never()).processSingleEvent(any());
  }

  @Test
  void processOutboxEvents_ProcessorErrorContinues() {
    OutboxEvent event1 = new OutboxEvent();
    event1.setId(1L);
    event1.setOutboxStatus(OutboxStatus.PENDING);

    OutboxEvent event2 = new OutboxEvent();
    event2.setId(2L);
    event2.setOutboxStatus(OutboxStatus.PENDING);

    when(outboxRepository.findTop50ByOutboxStatusOrderByCreatedAtAsc(OutboxStatus.PENDING))
        .thenReturn(List.of(event1, event2));

    doThrow(new RuntimeException("Processing error"))
        .when(outboxProcessor)
        .processSingleEvent(event1);

    outboxScheduler.processOutboxEvents();

    verify(outboxProcessor).processSingleEvent(event1);
    verify(outboxProcessor).processSingleEvent(event2);
  }
}

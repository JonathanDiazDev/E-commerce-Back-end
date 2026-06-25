package com.jonathan.ecommerce.kafka.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderKafkaConsumerTest {

  @Mock private EmailService emailService;
  @Mock private ProcessedEventRepository processedEventRepository;

  @InjectMocks private OrderKafkaConsumer orderKafkaConsumer;

  @Test
  void consumeOrderEvent_Success() throws MessagingException {
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

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);

    orderKafkaConsumer.consumeOrderEvent(event);

    verify(emailService).sendOrderConfirmationEmail(event);
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  @Test
  void consumeOrderEvent_DuplicateEvent() throws MessagingException {
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

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(true);

    orderKafkaConsumer.consumeOrderEvent(event);

    verify(emailService, never()).sendOrderConfirmationEmail(any());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void consumeOrderEvent_MessagingException() throws MessagingException {
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

    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);
    doThrow(new MessagingException("Email failed"))
        .when(emailService)
        .sendOrderConfirmationEmail(any());

    org.junit.jupiter.api.Assertions.assertThrows(
        MessagingException.class, () -> orderKafkaConsumer.consumeOrderEvent(event));

    verify(processedEventRepository, never()).save(any());
  }
}

package com.jonathan.ecommerce.kafka.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.PaymentRetryEvent;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.entity.enums.EventType;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.PaymentService;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentRetryConsumer {

  private final PaymentService paymentService;
  private final OrderRepository orderRepository;
  private final ProcessedEventRepository processedEventRepository;
  private final ObjectMapper objectMapper;

  // Spring will retry up to 6 times, waiting 5 seconds between each attempt.
  // It will only retry if the thrown exception is a PaymentException.
  @RetryableTopic(
      attempts = "6",
      backoff = @Backoff(delay = 5000, multiplier = 2.0),
      include = {PaymentException.class})
  @KafkaListener(topics = KafkaConfig.PAYMENT_RETRY_TOPIC, groupId = "payment-retry-group")
  public void consumePaymentRetry(PaymentRetryEvent event)
      throws ResourceNotFoundException, PaymentException {

    log.info("Processing payment retry for order {}", event.orderId());

    Order order =
        orderRepository
            .findById(event.orderId())
            .orElseThrow(
                () ->
                    new ResourceNotFoundException(
                        "Order not found for order id " + event.orderId()));

    if (order.getOrderStatus() == OrderStatus.PAID) {
      log.info("Order {} is already paid. Skipping retry.", event.orderId());
      return;
    }
    if (!processedEventRepository.existsByIdemKey(event.messageId())) {
      ProcessedEvent processedEvent = new ProcessedEvent();
      processedEvent.setIdemKey(event.messageId());
      processedEvent.setEventType(EventType.PAYMENT_RETRY);
      processedEvent.setEmail(order.getUser().getEmail());
      processedEvent.setProcessedAt(Instant.now());

      paymentService.processPayment(order, event.paymentMethodId());
      processedEventRepository.save(processedEvent);

      log.info("Payment retry successful for order {}", event.orderId());
    }
  }
}

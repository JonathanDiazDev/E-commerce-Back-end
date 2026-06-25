package com.jonathan.ecommerce.kafka.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.event.PaymentRetryEvent;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.ProcessedEvent;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.service.PaymentService;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRetryConsumerTest {

  @Mock private PaymentService paymentService;
  @Mock private OrderRepository orderRepository;
  @Mock private ProcessedEventRepository processedEventRepository;
  @Mock private ObjectMapper objectMapper;

  @InjectMocks private PaymentRetryConsumer paymentRetryConsumer;

  private PaymentRetryEvent event;
  private Order order;

  @BeforeEach
  void setUp() {
    event = new PaymentRetryEvent(1L, "pm_card_visa", 1, "msg-1", Instant.now());

    order = new Order();
    order.setId(1L);
    order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    User user = new User();
    user.setEmail("test@test.com");
    order.setUser(user);
  }

  @Test
  void consumePaymentRetry_Success() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);

    paymentRetryConsumer.consumePaymentRetry(event);

    verify(paymentService).processPayment(order, "pm_card_visa");
    verify(processedEventRepository).save(any(ProcessedEvent.class));
  }

  @Test
  void consumePaymentRetry_OrderNotFound() {
    when(orderRepository.findById(1L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        ResourceNotFoundException.class, () -> paymentRetryConsumer.consumePaymentRetry(event));

    verify(paymentService, never()).processPayment(any(), anyString());
  }

  @Test
  void consumePaymentRetry_AlreadyPaid() {
    order.setOrderStatus(OrderStatus.PAID);
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    paymentRetryConsumer.consumePaymentRetry(event);

    verify(paymentService, never()).processPayment(any(), anyString());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void consumePaymentRetry_DuplicateEvent() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(true);

    paymentRetryConsumer.consumePaymentRetry(event);

    verify(paymentService, never()).processPayment(any(), anyString());
    verify(processedEventRepository, never()).save(any());
  }

  @Test
  void consumePaymentRetry_PaymentException() {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(processedEventRepository.existsByIdemKey("msg-1")).thenReturn(false);
    doThrow(new PaymentException("Stripe error"))
        .when(paymentService)
        .processPayment(any(), anyString());

    org.junit.jupiter.api.Assertions.assertThrows(
        PaymentException.class, () -> paymentRetryConsumer.consumePaymentRetry(event));
  }
}

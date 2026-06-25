package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.Payment;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.repository.PaymentRepository;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceImplTest {

  @Mock private OrderRepository orderRepository;
  @Mock private PaymentRepository paymentRepository;
  @Mock private OutboxRepository outboxRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private EventDataObjectDeserializer eventDataObjectDeserializer;

  @InjectMocks private StripeWebhookServiceImpl stripeWebhookService;

  private Payment payment;
  private Order order;

  @BeforeEach
  void setUp() {
    ReflectionTestUtils.setField(stripeWebhookService, "webhookSecret", "whsec_test");
    ReflectionTestUtils.setField(stripeWebhookService, "stripeApiKey", "sk_test");

    payment = new Payment();
    payment.setId(1L);
    payment.setStripePaymentIntentId("pi_123");
    payment.setPaymentStatus(PaymentStatus.PENDING);

    order = new Order();
    order.setId(1L);
    order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    order.setPayment(payment);
    order.setUser(new User());
    order.setTotalAmount(java.math.BigDecimal.valueOf(100));
    payment.setOrder(order);
  }

  @Test
  void handlePaymentSucceeded_Success() throws Exception {
    PaymentIntent paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getId()).thenReturn("pi_123");
    when(paymentIntent.getStatus()).thenReturn("succeeded");

    when(paymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(orderRepository.existsByStripePaymentIdAndOrderStatus("pi_123", OrderStatus.PAID))
        .thenReturn(false);
    when(objectMapper.writeValueAsString(any())).thenReturn("{}");

    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(paymentIntent));
    Event event = mock(Event.class);
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);

    stripeWebhookService.handlePaymentSucceeded(event);

    verify(paymentRepository).save(any(Payment.class));
    verify(orderRepository).saveAndFlush(any(Order.class));
    verify(outboxRepository).save(any(OutboxEvent.class));
  }

  @Test
  void handlePaymentSucceeded_AlreadyProcessed() {
    when(orderRepository.existsByStripePaymentIdAndOrderStatus("pi_123", OrderStatus.PAID))
        .thenReturn(true);

    PaymentIntent paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getId()).thenReturn("pi_123");

    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(paymentIntent));
    Event event = mock(Event.class);
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);

    stripeWebhookService.handlePaymentSucceeded(event);

    verify(paymentRepository, never()).save(any());
  }

  @Test
  void handlePaymentFailed_Success() {
    PaymentIntent paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getId()).thenReturn("pi_123");
    when(paymentIntent.getStatus()).thenReturn("requires_payment_method");

    when(paymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(paymentIntent));
    Event event = mock(Event.class);
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);

    stripeWebhookService.handlePaymentFailed(event);

    verify(paymentRepository).save(any(Payment.class));
    verify(orderRepository).saveAndFlush(order);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.FAILED);
  }

  @Test
  void handlePaymentCancelled_Success() {
    PaymentIntent paymentIntent = mock(PaymentIntent.class);
    when(paymentIntent.getId()).thenReturn("pi_123");
    when(paymentIntent.getStatus()).thenReturn("canceled");

    when(paymentRepository.findByStripePaymentIntentId("pi_123")).thenReturn(Optional.of(payment));
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    when(eventDataObjectDeserializer.getObject()).thenReturn(Optional.of(paymentIntent));
    Event event = mock(Event.class);
    when(event.getDataObjectDeserializer()).thenReturn(eventDataObjectDeserializer);

    stripeWebhookService.handlePaymentCancelled(event);

    verify(paymentRepository).save(any(Payment.class));
    verify(orderRepository).saveAndFlush(order);
    assertThat(order.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  void validateWebhookSignature_InvalidSignature() {
    try (var mockedWebhook = mockStatic(Webhook.class)) {
      mockedWebhook
          .when(() -> Webhook.constructEvent(anyString(), anyString(), anyString()))
          .thenThrow(new com.stripe.exception.SignatureVerificationException("Invalid sig", null));

      boolean result = stripeWebhookService.validateWebhookSignature("payload", "bad_sig");

      assertThat(result).isFalse();
    }
  }
}

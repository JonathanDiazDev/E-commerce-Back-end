package com.jonathan.ecommerce.service.impl;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.PaymentRequest;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.Payment;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.PaymentAttemptRepository;
import com.jonathan.ecommerce.repository.PaymentRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StripePaymentServiceTest {

  @Mock private PaymentRepository paymentRepository;
  @Mock private StripeClient stripeClient;
  @Mock private PaymentAttemptRepository paymentAttemptRepository;
  @Mock private OrderRepository orderRepository;

  @InjectMocks private StripePaymentService stripePaymentService;

  private Order order;

  @BeforeEach
  void setUp() {
    order = new Order();
    order.setId(1L);
    order.setTotalAmount(BigDecimal.valueOf(500));
    order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    order.setUser(new User());
  }

  @Test
  void processPayment_Success() throws StripeException {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));

    PaymentIntent intent = mock(PaymentIntent.class);
    when(intent.getId()).thenReturn("pi_123");
    when(intent.getStatus()).thenReturn("succeeded");
    when(stripeClient.paymentIntents())
        .thenReturn(mock(com.stripe.service.PaymentIntentService.class));
    when(stripeClient.paymentIntents().create(any(PaymentIntentCreateParams.class)))
        .thenReturn(intent);
    when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());

    Payment result = stripePaymentService.processPayment(order, "pm_card_visa");

    org.assertj.core.api.Assertions.assertThat(result).isNotNull();
    org.assertj.core.api.Assertions.assertThat(result.getPaymentStatus())
        .isEqualTo(PaymentStatus.SUCCESS);
    org.assertj.core.api.Assertions.assertThat(result.getStripePaymentIntentId())
        .isEqualTo("pi_123");

    verify(orderRepository, times(2)).save(any(Order.class));
  }

  @Test
  void processPayment_OrderNotFound() {
    when(orderRepository.findById(1L)).thenReturn(Optional.empty());

    org.junit.jupiter.api.Assertions.assertThrows(
        PaymentException.class, () -> stripePaymentService.processPayment(order, "pm_card_visa"));
  }

  @Test
  void processPayment_StripeException() throws StripeException {
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
    when(paymentRepository.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
    when(stripeClient.paymentIntents())
        .thenReturn(mock(com.stripe.service.PaymentIntentService.class));
    when(stripeClient.paymentIntents().create(any(PaymentIntentCreateParams.class)))
        .thenThrow(new StripeException("Card declined", "pm_123", "400", 400) {});

    org.junit.jupiter.api.Assertions.assertThrows(
        PaymentException.class, () -> stripePaymentService.processPayment(order, "pm_card_visa"));

    verify(paymentAttemptRepository).save(any());
  }

  @Test
  void confirmPayment_Success() throws StripeException {
    PaymentRequest request = new PaymentRequest(1L, "pm_card_visa", BigDecimal.valueOf(500));

    try (var mockedIntent = mockStatic(PaymentIntent.class)) {
      PaymentIntent intent = mock(PaymentIntent.class);
      when(intent.getId()).thenReturn("pi_123");
      mockedIntent
          .when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
          .thenReturn(intent);
      when(paymentRepository.findByOrderId(1L)).thenReturn(Optional.empty());
      when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

      stripePaymentService.confirmPayment(request);

      verify(orderRepository).save(argThat(o -> o.getOrderStatus() == OrderStatus.PAID));
    }
  }

  @Test
  void confirmPayment_StripeException() throws StripeException {
    PaymentRequest request = new PaymentRequest(1L, "pm_card_visa", BigDecimal.valueOf(500));

    try (var mockedIntent = mockStatic(PaymentIntent.class)) {
      mockedIntent
          .when(() -> PaymentIntent.create(any(PaymentIntentCreateParams.class)))
          .thenThrow(new StripeException("Card declined", "pm_123", "400", 400) {});

      org.junit.jupiter.api.Assertions.assertThrows(
          RuntimeException.class, () -> stripePaymentService.confirmPayment(request));
    }
  }
}

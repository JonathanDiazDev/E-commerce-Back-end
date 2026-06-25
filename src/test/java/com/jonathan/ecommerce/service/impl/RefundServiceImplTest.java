package com.jonathan.ecommerce.service.impl;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.event.RefundRequest;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.Payment;
import com.jonathan.ecommerce.entity.User;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RefundServiceImplTest {

  @Mock private OrderRepository orderRepository;
  @Mock private StripeClient stripeClient;

  @InjectMocks private RefundServiceImpl refundService;

  private Order order;
  private Payment payment;

  @BeforeEach
  void setUp() {
    payment = new Payment();
    payment.setId(1L);
    payment.setStripePaymentIntentId("pi_123");

    order = new Order();
    order.setId(1L);
    order.setOrderStatus(OrderStatus.PAID);
    order.setPayment(payment);
    order.setUser(new User());
  }

  @Test
  void processRefund_Success() throws StripeException {
    RefundRequest request = new RefundRequest(1L, "item damaged");

    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    try (var mockedStatic = mockStatic(Refund.class)) {
      Refund refund = mock(Refund.class);
      when(refund.getId()).thenReturn("refund_123");
      mockedStatic
          .when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
          .thenReturn(refund);

      refundService.processRefund(request);

      verify(orderRepository).save(order);
      org.assertj.core.api.Assertions.assertThat(order.getOrderStatus())
          .isEqualTo(OrderStatus.REFUNDED);
    }
  }

  @Test
  void processRefund_OrderNotFound() {
    RefundRequest request = new RefundRequest(999L, "reason");
    when(orderRepository.findById(999L)).thenReturn(Optional.empty());

    assertThrows(ResourceNotFoundException.class, () -> refundService.processRefund(request));
  }

  @Test
  void processRefund_InvalidOrderStatus() {
    order.setOrderStatus(OrderStatus.PENDING_PAYMENT);
    RefundRequest request = new RefundRequest(1L, "reason");
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    assertThrows(IllegalArgumentException.class, () -> refundService.processRefund(request));
  }

  @Test
  void processRefund_StripeException() throws StripeException {
    RefundRequest request = new RefundRequest(1L, "reason");
    when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

    try (var mockedStatic = mockStatic(Refund.class)) {
      mockedStatic
          .when(() -> Refund.create(any(RefundCreateParams.class), any(RequestOptions.class)))
          .thenThrow(new StripeException("Stripe error", "param", "400", 400) {});

      assertThrows(PaymentException.class, () -> refundService.processRefund(request));
    }
  }
}

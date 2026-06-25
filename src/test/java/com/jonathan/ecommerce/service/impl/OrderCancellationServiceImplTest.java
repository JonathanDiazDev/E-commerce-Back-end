package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.repository.OrderRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OrderCancellationServiceImplTest {

  @Mock private OrderRepository orderRepository;

  @InjectMocks private OrderCancellationServiceImpl orderCancellationService;

  @Test
  void cancelExpiredOrders_Success() {
    Order order1 = new Order();
    order1.setId(1L);
    order1.setOrderStatus(OrderStatus.PENDING_PAYMENT);

    Order order2 = new Order();
    order2.setId(2L);
    order2.setOrderStatus(OrderStatus.PENDING_PAYMENT);

    when(orderRepository.findExpiredPendingPaymentOrders(any(Instant.class)))
        .thenReturn(List.of(order1, order2));

    orderCancellationService.cancelExpiredOrders();

    verify(orderRepository, times(2)).save(any(Order.class));
    assertThat(order1.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(order2.getOrderStatus()).isEqualTo(OrderStatus.CANCELLED);
  }

  @Test
  void cancelExpiredOrders_NoExpiredOrders() {
    when(orderRepository.findExpiredPendingPaymentOrders(any(Instant.class))).thenReturn(List.of());

    orderCancellationService.cancelExpiredOrders();

    verify(orderRepository, never()).save(any());
  }
}

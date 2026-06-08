package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.service.OrderCancellationService;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCancellationServiceImpl implements OrderCancellationService {
  private final OrderRepository orderRepository;

  @Override
  @Scheduled(cron = "0 0 0 * * *")
  @Transactional
  public void cancelExpiredOrders() {
    Instant cutoffTime = Instant.now().minus(Duration.ofDays(7));
    List<Order> expiredOrders = orderRepository.findExpiredPendingPaymentOrders(cutoffTime);

    if (expiredOrders.isEmpty()) {
      log.info("No expired orders found to cancel");
      return;
    }

    for (Order order : expiredOrders) {
      order.setOrderStatus(OrderStatus.CANCELLED);
      orderRepository.save(order);
      log.info("Order {} cancelled due to payment timeout", order.getId());
    }

    log.info("✅ Successfully cancelled {} expired orders", expiredOrders.size());
  }
}

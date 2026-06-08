package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.event.RefundRequest;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.service.RefundService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefundServiceImpl implements RefundService {

  private final OrderRepository orderRepository;
  private final StripeClient stripeClient;

  @Override
  @Transactional
  public void processRefund(RefundRequest request) {

    Order order =
        orderRepository
            .findById(request.orderId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    OrderStatus status = order.getOrderStatus();

    if (status != OrderStatus.PAID
        && status != OrderStatus.SHIPPED
        && status != OrderStatus.DELIVERED) {
      throw new IllegalArgumentException("Order cannot be refunded in status: " + status);
    }

    String idempotencyKey =
        "refund-order-" + request.orderId() + "-" + Instant.now().getEpochSecond();

    try {

      RefundCreateParams params =
          RefundCreateParams.builder()
              .setCharge(order.getPayment().getStripePaymentIntentId())
              .setReason(RefundCreateParams.Reason.REQUESTED_BY_CUSTOMER)
              .setMetadata(Map.of("orderId", order.getId().toString()))
              .build();

      Refund refund =
          Refund.create(params, RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
      order.setOrderStatus(OrderStatus.REFUNDED);

      orderRepository.save(order);

      log.info(
          "Refund has been created for order {} with refund ID: {}", order.getId(), refund.getId());

    } catch (StripeException e) {
      throw new PaymentException("Refund failed: " + e.getMessage());
    }
  }
}

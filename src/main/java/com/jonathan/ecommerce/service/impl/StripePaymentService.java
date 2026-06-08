package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.request.PaymentRequest;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.Payment;
import com.jonathan.ecommerce.entity.PaymentAttempt;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.PaymentAttemptRepository;
import com.jonathan.ecommerce.repository.PaymentRepository;
import com.jonathan.ecommerce.service.PaymentService;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.param.PaymentIntentCreateParams;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class StripePaymentService implements PaymentService {

  private final PaymentRepository paymentRepository;
  private final StripeClient stripeClient;
  private final PaymentAttemptRepository paymentAttemptRepository;
  private final OrderRepository orderRepository;

  @Override
  @Transactional
  public Payment processPayment(Order order, String paymentMethodId) {

    Order managedOrder =
        orderRepository
            .findById(order.getId())
            .orElseThrow(() -> new PaymentException("Order not found"));

    Payment payment = new Payment();
    payment.setOrder(managedOrder);
    payment.setAmount(managedOrder.getTotalAmount());
    payment.setPaymentStatus(PaymentStatus.PENDING);
    paymentRepository.save(payment);

    try {
      // Crear y confirmar en un solo paso es más seguro
      PaymentIntentCreateParams params =
          PaymentIntentCreateParams.builder()
              .setAmount(managedOrder.getTotalAmount().multiply(new BigDecimal(100)).longValue())
              .setCurrency("usd")
              .setPaymentMethod(paymentMethodId) // <--- Validación de Stripe ocurre aquí
              .setConfirm(true) // Confirma inmediatamente
              .setAutomaticPaymentMethods(
                  PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                      .setEnabled(true)
                      .setAllowRedirects(
                          PaymentIntentCreateParams.AutomaticPaymentMethods.AllowRedirects.NEVER)
                      .build())
              .build();

      PaymentIntent intent = stripeClient.paymentIntents().create(params);
      PaymentStatus newStatus = mapStripeStatus(intent.getStatus());

      if ("succeeded".equals(intent.getStatus())) {
        updateDatabaseWithPaymentSuccess(managedOrder.getId(), intent.getId());
        payment.setStripePaymentIntentId(intent.getId());
        payment.setPaymentStatus(PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        managedOrder.setOrderStatus(OrderStatus.PAID);
        managedOrder.setStripePaymentId(intent.getId());
        managedOrder.setPayment(payment);
        orderRepository.save(managedOrder);
        log.info("Payment successful and order updated: {}", managedOrder.getId());
      } else {
        payment.setStripePaymentIntentId(intent.getId());
        payment.setPaymentStatus(newStatus);
        paymentRepository.save(payment);
        saveIntent(payment, intent.getId(), newStatus.name(), null);
      }

      return payment;
    } catch (StripeException e) {
      saveIntent(payment, null, "FAILED", e.getUserMessage());
      throw new PaymentException("Error en el pago: " + e.getUserMessage());
    }
  }

  @Override
  public void confirmPayment(PaymentRequest request) {

    PaymentIntentCreateParams params =
        PaymentIntentCreateParams.builder()
            .setAmount(request.amount().multiply(new BigDecimal(100)).longValue())
            .setCurrency("usd")
            .setPaymentMethod(request.paymentMethodId()) // <-- Validación real aquí
            .setConfirm(true) // Confirma el pago
            .build();

    PaymentIntent intent;
    try {
      intent = PaymentIntent.create(params);
    } catch (StripeException e) {
      throw new RuntimeException(e);
    }

    // Solo si llegamos aquí, el pago es real
    updateDatabaseWithPaymentSuccess(request.orderId(), intent.getId());
  }

  private void saveIntent(Payment payment, String transactionId, String status, String error) {
    PaymentAttempt attempt = new PaymentAttempt();
    attempt.setPayment(payment);
    attempt.setAttemptNumber(1);
    attempt.setStatus(PaymentStatus.valueOf(status));
    attempt.setTransactionId(transactionId);
    attempt.setErrorMessage(error);
    paymentAttemptRepository.save(attempt);
  }

  private void updateDatabaseWithPaymentSuccess(Long orderId, String paymentIntentId) {
    // 1. Buscamos la orden
    Order order =
        orderRepository
            .findById(orderId)
            .orElseThrow(() -> new PaymentException("Orden no encontrada: " + orderId));

    // 2. Buscamos o creamos el registro de pago
    Payment payment = paymentRepository.findByOrderId(orderId).orElse(new Payment());

    // 3. Actualizamos los datos
    payment.setOrder(order);
    payment.setStripePaymentIntentId(paymentIntentId);
    payment.setPaymentStatus(PaymentStatus.SUCCESS);

    // 4. Guardamos
    paymentRepository.save(payment);

    // 5. Actualizamos el estado de la orden
    log.info(
        "DEBUG: Guardando Orden {} con StripeID {}",
        order.getId(),
        payment.getStripePaymentIntentId());
    order.setOrderStatus(OrderStatus.PAID);
    orderRepository.save(order);

    log.info("Orden {} actualizada a PAID con el PaymentIntent {}", orderId, paymentIntentId);
  }

  private PaymentStatus mapStripeStatus(String stripeStatus) {
    return switch (stripeStatus) {
      case "succeeded" -> PaymentStatus.SUCCESS;
      case "processing" -> PaymentStatus.PENDING;
      case "requires_payment_method" -> PaymentStatus.PENDING;
      case "requires_action" -> PaymentStatus.PENDING;
      case "canceled" -> PaymentStatus.FAILED;
      default -> {
        log.warn("Estado de Stripe desconocido recibido: {}", stripeStatus);
        yield PaymentStatus.FAILED;
      }
    };
  }
}

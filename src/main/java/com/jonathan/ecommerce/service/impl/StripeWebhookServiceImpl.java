package com.jonathan.ecommerce.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.jonathan.ecommerce.dto.enums.OutboxStatus;
import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.entity.OutboxEvent;
import com.jonathan.ecommerce.entity.Payment;
import com.jonathan.ecommerce.entity.enums.OrderStatus;
import com.jonathan.ecommerce.entity.enums.PaymentStatus;
import com.jonathan.ecommerce.exception.PaymentException;
import com.jonathan.ecommerce.exception.ResourceNotFoundException;
import com.jonathan.ecommerce.repository.OrderRepository;
import com.jonathan.ecommerce.repository.OutboxRepository;
import com.jonathan.ecommerce.repository.PaymentRepository;
import com.jonathan.ecommerce.service.StripeWebhookService;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.ApiResource;
import com.stripe.net.Webhook;
import jakarta.annotation.PostConstruct;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
public class StripeWebhookServiceImpl implements StripeWebhookService {
  @Value("${stripe.webhook.secret}")
  private String webhookSecret;

  @Value("${stripe.secret.key}")
  private String stripeApiKey;

  private final OrderRepository orderRepository;
  private final PaymentRepository paymentRepository;
  private final OutboxRepository outboxRepository;
  private final ObjectMapper objectMapper;

  @PostConstruct
  public void init() {
    Stripe.apiKey = stripeApiKey;
  }

  @Override
  public boolean validateWebhookSignature(String payload, String sigHeader) {
    try {
      Event event = Webhook.constructEvent(payload, sigHeader, webhookSecret);

      log.info("Validity of the Stripe Webhook signature event: {}", event.getType());

      handleEvent(event);
      return true;
    } catch (SignatureVerificationException e) {
      log.error("Firma de webhook inválida: {}", e.getMessage());
      return false;
    } catch (Exception e) {
      log.error("Error procesando webhook: {}", e.getMessage());
      return false;
    }
  }

  @Override
  public void handleEvent(Event event) {
    String eventType = event.getType();

    switch (eventType) {
      case "payment_intent.succeeded":
        handlePaymentSucceeded(event);
        break;
      case "payment_intent.payment_failed":
        handlePaymentFailed(event);
        break;
      case "payment_intent.canceled":
        handlePaymentCancelled(event);
        break;
      default:
        log.info("Evento ignorado: {}", eventType); // Cambiado a INFO para no ensuciar con WARN
    }
  }

  @Override
  @Transactional
  public void handlePaymentSucceeded(Event event) {
    PaymentIntent paymentIntent = extractPaymentIntent(event);

    if (orderRepository.existsByStripePaymentIdAndOrderStatus(
        paymentIntent.getId(), OrderStatus.PAID)) {
      log.info("The payment {} has already been processed. Ignoring event.", paymentIntent.getId());
      return;
    }
    updatePaymentAndOrder(extractPaymentIntent(event), OrderStatus.PAID);
  }

  @Override
  public void handlePaymentFailed(Event event) {
    updatePaymentAndOrder(extractPaymentIntent(event), OrderStatus.FAILED);
  }

  @Override
  public void handlePaymentCancelled(Event event) {
    updatePaymentAndOrder(extractPaymentIntent(event), OrderStatus.CANCELLED);
  }

  @Transactional
  private void updatePaymentAndOrder(PaymentIntent intent, OrderStatus orderStatus) {
    Payment payment =
        paymentRepository
            .findByStripePaymentIntentId(intent.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Payment not found in DB"));

    if (payment.getPaymentStatus() == PaymentStatus.SUCCESS) {
      log.info("Pago {} ya marcado como exitoso, omitiendo procesamiento.", payment.getId());
      return;
    }

    payment.setPaymentStatus(PaymentStatus.fromStripe(intent.getStatus()));
    paymentRepository.save(payment);

    Order order =
        orderRepository
            .findById(payment.getOrder().getId())
            .orElseThrow(() -> new ResourceNotFoundException("Order not found in DB"));
    order.setOrderStatus(orderStatus);
    order.setStripePaymentId(intent.getId());
    order.setPayment(payment);
    orderRepository.saveAndFlush(order);

    List<String> products =
        order.getItems().stream().map(item -> item.getProduct().getName()).toList();
    String messageId = UUID.randomUUID().toString();

    if (orderStatus == OrderStatus.PAID) {
      try {
        OrderPlacedEvent event =
            new OrderPlacedEvent(
                order.getUser().getEmail(),
                order.getUser().getName(),
                order.getId(),
                products,
                order.getTotalAmount(),
                messageId,
                "Por confirmar",
                "3-5 días hábiles");

        log.info("Total amount of this order is: {}", event.totalAmount());
        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setPayload(objectMapper.writeValueAsString(event));
        outboxEvent.setOutboxStatus(OutboxStatus.PENDING);

        outboxRepository.save(outboxEvent);
        log.info("Evento de Outbox registrado para el pedido: {}", order.getId());
      } catch (Exception e) {
        log.error("Error al registrar el evento en el Outbox", e);
      }
    }
    log.info("Payment and Order {} updated to {}", order.getId(), orderStatus);
  }

  private PaymentIntent extractPaymentIntent(Event event) {
    // 1. Intentamos obtener el objeto del evento de la forma nativa
    return event
        .getDataObjectDeserializer()
        .getObject()
        .filter(obj -> obj instanceof PaymentIntent)
        .map(obj -> (PaymentIntent) obj)
        .orElseGet(
            () -> {
              // 2. Si es nulo o no es PaymentIntent, extraemos el ID manualmente
              // del JSON crudo que SIEMPRE está presente en el evento.
              try {
                String json = event.getData().getObject().toJson();
                JsonObject jsonObject = ApiResource.GSON.fromJson(json, JsonObject.class);
                String id = jsonObject.get("id").getAsString();

                // 3. Recuperamos el objeto fresco desde la API
                return PaymentIntent.retrieve(id);
              } catch (StripeException e) {
                log.error("Error al recuperar el PaymentIntent de la API de Stripe", e);
                throw new PaymentException("Error recuperando PaymentIntent: " + e.getMessage());
              }
            });
  }
}

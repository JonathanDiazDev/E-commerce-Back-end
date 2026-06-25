package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaProducer {
  private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

  public void sendOrderEvent(Order order) throws Exception {

    List<String> products =
        order.getItems().stream().map(item -> item.getProduct().getName()).toList();

    String messageId = UUID.randomUUID().toString();

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

    kafkaTemplate
        .send(KafkaConfig.ORDER_EVENT, order.getId().toString(), event)
        .whenComplete(
            (result, throwable) -> {
              if (throwable != null) {
                log.error("❌ Error sending order event", throwable);
              } else {
                log.info("✅ Order Event CONFIRMED by Kafka for {}", event.orderId());
              }
            });
  }

  public void sendOrderOutboxEvents(OrderPlacedEvent event) throws Exception {

    log.info(
        "Publishing event to Kafka. Topic: {}, Order: {}",
        KafkaConfig.ORDER_EVENT,
        event.orderId().toString());

    kafkaTemplate
        .send(KafkaConfig.ORDER_EVENT, event.orderId().toString(), event)
        .whenComplete(
            (res, err) -> {
              if (err == null) {
                log.info("✅ Event CONFIRMED by Kafka for {}", event.orderId());
              } else {
                log.error("❌ Error sending event {}", event.orderId(), err);
              }
            });
  }
}

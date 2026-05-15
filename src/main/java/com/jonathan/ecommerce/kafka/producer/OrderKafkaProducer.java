package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.entity.Order;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class OrderKafkaProducer {
    private final KafkaTemplate<String, OrderPlacedEvent> kafkaTemplate;

    public void sendOrderEvent(Order order) throws Exception {

        List<String> products = order.getItems().stream()
                .map(item -> item.getProduct().getName())
                .toList();

        OrderPlacedEvent event = new OrderPlacedEvent(
                order.getUser().getEmail(),
                order.getUser().getName(),
                order.getId(),
                products,
                order.getTotalAmount()
        );

        kafkaTemplate.send(KafkaConfig.ORDER_EVENT, order.getId().toString(), event).get();
    }

    public void sendOrderOutboxEvents(OrderPlacedEvent event) throws Exception {

        log.info("Publishing event to Kafka. Topic: {}, Order: {}", KafkaConfig.ORDER_EVENT, event.orderId().toString());

        kafkaTemplate.send(KafkaConfig.ORDER_EVENT, event.orderId().toString(), event).get();

        log.info("✅ Event CONFIRMED by Kafka for: {}", event.userEmail());
    }
}


package com.jonathan.ecommerce.kafka.consumer;

import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.dto.OrderPlacedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class OderKafkaConsumer {

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 5000), autoCreateTopics = "true")
    @KafkaListener(topics = KafkaConfig.ORDER_EVENT, groupId = "email-service-group")
    public void consumeOrderEvent(OrderPlacedEvent event) {
        log.info("Received order event {}", event);

    }

}

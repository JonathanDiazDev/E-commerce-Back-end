package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.event.PaymentRetryEvent;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import java.time.Instant;
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
public class PaymentRetryProducer {
  private final KafkaTemplate<String, PaymentRetryEvent> kafkaTemplate;

  public void sendPaymentRetryEvent(Long orderId, int attemptNumber, String paymentMethodId) {
    String messageId = UUID.randomUUID().toString();
    PaymentRetryEvent event =
        new PaymentRetryEvent(orderId, paymentMethodId, attemptNumber, messageId, Instant.now());

    kafkaTemplate.send(KafkaConfig.PAYMENT_RETRY_TOPIC, orderId.toString(), event);

    log.info("Payment retry event sent for order {}, attempt number {}", orderId, attemptNumber);
  }
}

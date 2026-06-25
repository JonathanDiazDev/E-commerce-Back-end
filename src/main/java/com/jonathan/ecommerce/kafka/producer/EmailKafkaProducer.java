package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@Slf4j
@RequiredArgsConstructor
public class EmailKafkaProducer {
  private final KafkaTemplate<String, EmailRequest> kafkaTemplate;

  public void sendEmailEvent(EmailRequest request) throws Exception {

    String key = request.to();

    log.info("Publishing event to Kafka. Topic: {}, Key: {}", KafkaConfig.EMAIL_TOPIC, key);

    kafkaTemplate
        .send(KafkaConfig.EMAIL_TOPIC, key, request)
        .whenComplete(
            (result, ex) -> {
              if (ex != null) {
                log.error("❌ Kafka send failed for key: {}", key, ex);
              } else {
                log.info("✅ Kafka ack received for key: {}", key);
              }
            });
  }
}

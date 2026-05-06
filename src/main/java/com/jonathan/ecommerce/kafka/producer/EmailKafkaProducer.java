package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class EmailKafkaProducer {
  private final KafkaTemplate<String, EmailRequest> kafkaTemplate;

  public void sendEmailEvent(EmailRequest request) throws Exception {

    String key = request.to();

    log.info("Publishing event to Kafka. Topic: {}, Key: {}", KafkaConfig.EMAIL_TOPIC, key);

    kafkaTemplate.send(KafkaConfig.EMAIL_TOPIC, key, request).get();

    log.info("✅ Event successfully sent to Kafka for: {}", key);
  }
}

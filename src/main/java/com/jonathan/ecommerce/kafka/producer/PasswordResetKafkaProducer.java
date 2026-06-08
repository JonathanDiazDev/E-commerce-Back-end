package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.event.PasswordResetEvent;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PasswordResetKafkaProducer {
  private final KafkaTemplate<String, PasswordResetEvent> kafkaTemplate;

  public void sendPasswordResetEvent(PasswordResetEvent event) {

    kafkaTemplate.send(KafkaConfig.PASSWORD_RESET_TOPIC, event.email(), event);
  }
}

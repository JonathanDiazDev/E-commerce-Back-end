package com.jonathan.ecommerce.kafka.producer;

import com.jonathan.ecommerce.dto.event.UserRegisteredEvent;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Profile("!test")
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredKafkaProducer {
  private final KafkaTemplate<String, UserRegisteredEvent> kafkaTemplate;

  public void sendEmailUserRegistered(UserRegisteredEvent event) {

    kafkaTemplate
        .send(KafkaConfig.USER_REGISTERED_TOPIC, event.email(), event)
        .whenComplete(
            (result, error) -> {
              if (error != null) {
                log.error("Error while sending email to topic", error);
              }
            });

    log.info("Email Sent to UserRegistered Topic for {}", event.userName());
  }
}

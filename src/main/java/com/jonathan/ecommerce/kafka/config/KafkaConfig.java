package com.jonathan.ecommerce.kafka.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@Configuration
public class KafkaConfig {

  public static final String EMAIL_TOPIC = "stock-emails-topic";
  public static final String ORDER_EVENT = "order-emails-topic";
  public static final String PAYMENT_RETRY_TOPIC = "payment-retry-topic";
  public static final String PAYMENT_RETRY_DLQ_TOPIC = "payment-retry-dlq";
  public static final String PASSWORD_RESET_TOPIC = "password-reset-topic";
  public static final String USER_REGISTERED_TOPIC = "user-registered-topic";

  @Bean
  public NewTopic emailTopic() {
    return TopicBuilder.name(EMAIL_TOPIC).partitions(3).replicas(1).build();
  }

  @Bean
  public NewTopic paymentRetryTopic() {
    return TopicBuilder.name(PAYMENT_RETRY_TOPIC).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic paymentRetryDlqTopic() {
    return TopicBuilder.name(PAYMENT_RETRY_DLQ_TOPIC).partitions(1).replicas(1).build();
  }

  @Bean
  public NewTopic userRegisteredTopic() {
    return TopicBuilder.name(USER_REGISTERED_TOPIC).build();
  }

  @Bean
  public NewTopic passwordResetTopic() {
    return TopicBuilder.name(PASSWORD_RESET_TOPIC).build();
  }

  @Bean
  public NewTopic orderEmailTopic() {
    return TopicBuilder.name(ORDER_EVENT).build();
  }
}

package com.jonathan.ecommerce.kafka.config;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.testcontainers.shaded.org.awaitility.Awaitility.await;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.kafka.consumer.EmailKafkaConsumer;
import com.jonathan.ecommerce.kafka.producer.EmailKafkaProducer;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.repository.ProcessedEventRepository;
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.EmailService;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(
    classes = {
      EmailKafkaConsumer.class,
      EmailKafkaProducer.class,
      KafkaConfig.class,
      EmailKafkaConsumerIntegrationTest.TestConfig.class
    },
    properties = "spring.kafka.consumer.group-id=test-group-unique")
// Mantenemos las propiedades de Kafka y desactivamos lo demás
@TestPropertySource(
    properties = {
      "spring.autoconfigure.exclude="
          + "org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration,"
          + "org.springframework.boot.autoconfigure.mail.MailAutoConfiguration"
    })
@Testcontainers
@ActiveProfiles("dev")
class EmailKafkaConsumerIntegrationTest {

  @Container @ServiceConnection
  static KafkaContainer kafkaContainer =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.4.0"));

  @Autowired private EmailKafkaProducer emailKafkaProducer;

  @Configuration
  @EnableAutoConfiguration
  static class TestConfig {}

  @MockitoBean private EmailService emailService;

  @MockitoBean private RefreshTokenRepository tokenRepository;

  @MockitoBean private UserRepository userRepository;

  @MockitoBean private RefreshTokenRepository refreshTokenRepository;

  @MockitoBean private FailedEmailRepository failedEmailRepository;

  @MockitoBean private ProcessedEventRepository processedEventRepository;

  @Test
  void shouldProcessEmailEventFromKafka() throws Exception {
    // 1. Preparamos un request que sabemos que es VÁLIDO
    EmailRequest request =
        new EmailRequest(
            "test@mail.com",
            "Jonathan",
            "Laptop",
            "STOCK_AVAILABILITY",
            UUID.randomUUID().toString(),
            Map.of("totalStock", 10));
    // 2. Enviamos el mensaje al Kafka real (Docker) y esperamos que el consumidor lo procese
    emailKafkaProducer.sendEmailEvent(request);

    // 3. Verificamos que el consumidor lo atrapó y llamó al servicio
    await()
        .atMost(5, TimeUnit.SECONDS)
        .untilAsserted(
            () -> {
              verify(emailService, times(1))
                  .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());
            });
  }
}

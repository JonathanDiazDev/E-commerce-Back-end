package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.kafka.producer.OrderKafkaProducer;
import com.jonathan.ecommerce.kafka.producer.PasswordResetKafkaProducer;
import com.jonathan.ecommerce.kafka.producer.PaymentRetryProducer;
import com.jonathan.ecommerce.kafka.producer.UserRegisteredKafkaProducer;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

  private static final PostgreSQLContainer<?> postgresContainer =
      new PostgreSQLContainer<>("postgres:16")
          .withDatabaseName("ecommerce")
          .withUsername("test")
          .withPassword("test");

  static {
    postgresContainer.start();
  }

  @MockitoBean protected OrderKafkaProducer orderKafkaProducer;
  @MockitoBean protected PasswordResetKafkaProducer passwordResetKafkaProducer;
  @MockitoBean protected PaymentRetryProducer paymentRetryProducer;
  @MockitoBean protected UserRegisteredKafkaProducer userRegisteredKafkaProducer;

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgresContainer::getJdbcUrl);
    registry.add("spring.datasource.username", postgresContainer::getUsername);
    registry.add("spring.datasource.password", postgresContainer::getPassword);
    registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
  }
}

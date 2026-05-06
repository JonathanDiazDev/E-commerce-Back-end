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
import com.jonathan.ecommerce.repository.RefreshTokenRepository;
import com.jonathan.ecommerce.repository.TokenRepository;
import com.jonathan.ecommerce.repository.UserRepository;
import com.jonathan.ecommerce.service.EmailService;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(classes = {
        // Solo cargamos la clase que consume los mensajes
        EmailKafkaConsumer.class,
        // Y las configuraciones necesarias de Kafka que tengas
        KafkaConfig.class

},
properties = "spring.kafka.consumer.group-id=test-group-unique"
)
// Mantenemos las propiedades de Kafka y desactivamos lo demás
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@Testcontainers
@ActiveProfiles("dev")
class EmailKafkaConsumerIntegrationTest {

    @Container
    @ServiceConnection
    static KafkaContainer kafkaContainer = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.4.0")
    );

    @MockitoBean
    private EmailService emailService;

    @MockitoBean
    private TokenRepository tokenRepository;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private RefreshTokenRepository refreshTokenRepository;

    @MockitoBean
    private FailedEmailRepository failedEmailRepository;

    @MockitoBean
    private EmailKafkaProducer emailKafkaProducer;

    @Test
    void shouldProcessEmailEventFromKafka() throws Exception {
        // 1. Preparamos un request que sabemos que es VÁLIDO
        EmailRequest request = new EmailRequest(
                "test@mail.com",
                "Laptop",
                "STOCK_AVAILABILITY",
                Map.of("totalStock", 10)
        );

        // 2. Enviamos el mensaje al Kafka real (Docker)
        Thread.sleep(10000);
        emailKafkaProducer.sendEmailEvent(request);

        // 3. Verificamos que el consumidor lo atrapó y llamó al servicio
        await()
                .atMost(5, TimeUnit.SECONDS)
                .untilAsserted(() -> {
                    verify(emailService, times(1))
                            .sendStockAvailabilityEmail(anyString(), anyString(), anyInt());
                });
    }

}

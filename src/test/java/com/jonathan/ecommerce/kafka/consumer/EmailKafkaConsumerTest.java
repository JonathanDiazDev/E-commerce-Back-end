package com.jonathan.ecommerce.kafka.consumer;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

@ExtendWith(MockitoExtension.class)
public class EmailKafkaConsumerTest {

  @Mock private EmailService emailService;

  @Mock private FailedEmailRepository failedEmailRepository;

  @InjectMocks private EmailKafkaConsumer emailKafkaConsumer;

  @Test
  void shouldSendEmailSuccessfullyWhenStockIsPresent() throws MessagingException {
    // 1. Arrange: Datos válidos
    EmailRequest request =
        new EmailRequest(
            "test@mail.com",
            "Jonathan",
            "Laptop",
            "STOCK_AVAILABILITY",
            UUID.randomUUID().toString(),
            Map.of("totalStock", 10));

    // 2. Act
    emailKafkaConsumer.listen(request);

    // 3. Assert: Verificamos que el servicio se llamó con los datos correctos
    verify(emailService, times(1))
        .sendStockAvailabilityEmail("test@mail.com", "Jonathan", "Laptop", 10);
  }

  @Test
  void shouldThrowExceptionWhenStockIsMissing() {
    // 1. Arrange: Datos inválidos (Mapa sin "totalStock")
    EmailRequest request =
        new EmailRequest(
            "test@mail.com",
            "Jonathan",
            "Laptop",
            "STOCK_AVAILABILITY",
            UUID.randomUUID().toString(),
            Map.of("totalStock", 10));

    assertThrows(
        IllegalArgumentException.class,
        () -> {
          emailKafkaConsumer.listen(request);
        });

    verifyNoInteractions(emailService);
  }

  @Test
  void shouldHandleEmailServiceError() throws MessagingException {
    EmailRequest request =
        new EmailRequest(
            "test@mail.com",
            "Jonathan",
            "Laptop",
            "STOCK_AVAILABILITY",
            UUID.randomUUID().toString(),
            Map.of("totalStock", 10));

    doThrow(new RuntimeException("SMTP Error"))
        .when(emailService)
        .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());

    assertThrows(
        RuntimeException.class,
        () -> {
          emailKafkaConsumer.listen(request);
        });
  }

  @Test
  void shouldHandleEmailSendException() throws MessagingException {
    EmailRequest request =
        new EmailRequest(
            "test@mail.com",
            "Jonathan",
            "Laptop",
            "STOCK_AVAILABILITY",
            UUID.randomUUID().toString(),
            Map.of("totalStock", 10));
    doThrow(new MailSendException("SMTP Error in connection"))
        .when(emailService)
        .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());
    assertThrows(
        MailSendException.class,
        () -> {
          emailKafkaConsumer.listen(request);
        });
  }
}

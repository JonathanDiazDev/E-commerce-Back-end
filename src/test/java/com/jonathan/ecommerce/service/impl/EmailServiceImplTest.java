package com.jonathan.ecommerce.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@ExtendWith(MockitoExtension.class)
class EmailServiceImplTest {

  @Mock private JavaMailSender mailSender;
  @Mock private TemplateEngine templateEngine;
  @Mock private MimeMessage mimeMessage;

  @InjectMocks private EmailServiceImpl emailService;

  @Captor private ArgumentCaptor<Context> contextCaptor;

  @BeforeEach
  void setUp() {
    lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
  }

  @Test
  void sendStockAvailabilityEmail_Success() throws Exception {
    when(templateEngine.process(eq("backInStockEmail"), contextCaptor.capture()))
        .thenReturn("<html/>");

    emailService.sendStockAvailabilityEmail("test@test.com", "Test User", "Product1", 10);

    Context capturedContext = contextCaptor.getValue();
    // Corregido a "userName", "product" y "stock" según tu lógica de negocio
    assertThat(capturedContext.getVariable("userName")).isEqualTo("Test User");
    assertThat(capturedContext.getVariable("product")).isEqualTo("Product1");
    assertThat(capturedContext.getVariable("stock")).isEqualTo(10);

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendAdminAlert_Success() {
    emailService.sendAdminAlert("Subject", "Message body");
    verify(mailSender).send(any(SimpleMailMessage.class));
  }

  @Test
  void sendRegisterEmail_Success() throws Exception {
    Instant now = Instant.now();
    when(templateEngine.process(eq("welcomeEmail"), contextCaptor.capture())).thenReturn("<html/>");

    emailService.sendRegisterEmail("test@test.com", "Test User", now);

    Context capturedContext = contextCaptor.getValue();
    assertThat(capturedContext.getVariable("userName")).isEqualTo("Test User");
    assertThat(capturedContext.getVariable("registrationDate")).isNotNull();
    assertThat(capturedContext.getVariable("supportEmail")).isEqualTo("jonadiazg30@gmail.com");

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendResetPasswordEmail_Success() throws Exception {
    when(templateEngine.process(eq("passwordResetEmail"), contextCaptor.capture()))
        .thenReturn("<html/>");

    emailService.sendResetPasswordEmail("test@test.com", "Test User", "http://reset-link");

    Context capturedContext = contextCaptor.getValue();
    assertThat(capturedContext.getVariable("userName")).isEqualTo("Test User");
    assertThat(capturedContext.getVariable("resetLink")).isEqualTo("http://reset-link");
    assertThat(capturedContext.getVariable("expirationMinutes")).isEqualTo(15);

    verify(mailSender).send(mimeMessage);
  }

  @Test
  void sendOrderConfirmationEmail_Success() throws Exception {
    when(templateEngine.process(eq("orderConfirmationEmail"), contextCaptor.capture()))
        .thenReturn("<html/>");

    OrderPlacedEvent event =
        new OrderPlacedEvent(
            "test@test.com",
            "Test User",
            1L,
            List.of("Product1"),
            BigDecimal.valueOf(100),
            "msg-1",
            "pending",
            "3-5 days");

    emailService.sendOrderConfirmationEmail(event);

    Context capturedContext = contextCaptor.getValue();
    // Corregido a "userName", "products" y "totalAmount" para hacer match con el evento
    assertThat(capturedContext.getVariable("userName")).isEqualTo("Test User");
    assertThat(capturedContext.getVariable("orderId")).isEqualTo(1L);
    assertThat(capturedContext.getVariable("products")).isEqualTo(List.of("Product1"));
    assertThat(capturedContext.getVariable("totalAmount")).isEqualTo(BigDecimal.valueOf(100));

    verify(mailSender).send(mimeMessage);
  }
}

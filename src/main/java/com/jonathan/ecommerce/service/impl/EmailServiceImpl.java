package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import com.jonathan.ecommerce.service.EmailService;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;
  private final TemplateEngine templateEngine;

  @Override
  @Retryable(
      retryFor = {MailSendException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000))
  public void sendStockAvailabilityEmail(
      String to, String userName, String productName, Integer stock) throws MessagingException {
    Context context = new Context();
    context.setVariable("userName", userName);
    context.setVariable("product", productName);
    context.setVariable("stock", stock);

    String htmlContent = templateEngine.process("backInStockEmail", context);

    MimeMessage message = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(message, "utf-8");

    helper.setTo(to);
    helper.setSubject("Stock Availability");
    helper.setFrom("jonadiazg30@gmail.com");
    helper.setText(htmlContent, true);
    mailSender.send(message);
  }

  @Override
  public void sendAdminAlert(String subject, String message) {
    SimpleMailMessage mailMessage = new SimpleMailMessage();
    mailMessage.setTo("Admin@ecommerce.com");
    mailMessage.setSubject(subject);
    mailMessage.setText(message);
    mailSender.send(mailMessage);
  }

  @Override
  public void sendRegisterEmail(String to, String name, Instant date) throws MessagingException {
    Context context = new Context();
    String formattedDate =
        DateTimeFormatter.ofPattern("d MMM yyyy")
            .withLocale(new Locale("es"))
            .withZone(ZoneId.of("America/Mexico_City"))
            .format(date);
    context.setVariable("registrationDate", formattedDate);
    context.setVariable("userName", name);
    context.setVariable("supportEmail", "jonadiazg30@gmail.com");

    String htmlContent = templateEngine.process("welcomeEmail", context);
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

    helper.setTo(to);
    helper.setSubject("¡Bienvenido!");
    helper.setFrom("jonadiazg30@gmail.com");
    helper.setText(htmlContent, true);
    mailSender.send(mimeMessage);
  }

  @Override
  public void sendResetPasswordEmail(String to, String userName, String resetLink)
      throws MessagingException {
    Context context = new Context();
    context.setVariable("userName", userName);
    context.setVariable("resetLink", resetLink);
    context.setVariable("expirationMinutes", 15);
    context.setVariable("supportEmail", "jonadiazg30@gmail.com");

    String htmlContent = templateEngine.process("passwordResetEmail", context);
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

    helper.setTo(to);
    helper.setSubject("Restablece tu contraseña, " + userName);
    helper.setFrom("jonadiazg30@gmail.com");
    helper.setText(htmlContent, true);
    mailSender.send(mimeMessage);
  }

  @Override
  public void sendOrderConfirmationEmail(OrderPlacedEvent event) throws MessagingException {
    Context context = new Context();
    context.setVariable("userName", event.userName());
    context.setVariable("orderId", event.orderId());
    context.setVariable("products", event.productNames());
    context.setVariable("totalAmount", event.totalAmount());

    String htmlContent = templateEngine.process("orderConfirmationEmail", context);
    MimeMessage mimeMessage = mailSender.createMimeMessage();
    MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "utf-8");

    helper.setTo(event.userEmail());
    helper.setSubject("¡Gracias por tu compra, " + event.userName() + "!");
    helper.setFrom("jonadiazg30@gmail.com");
    helper.setText(htmlContent, true);
    mailSender.send(mimeMessage);
  }
}

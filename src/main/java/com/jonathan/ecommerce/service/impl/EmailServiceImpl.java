package com.jonathan.ecommerce.service.impl;

import com.jonathan.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

  private final JavaMailSender mailSender;

  @Override
  @Retryable(
      retryFor = {MailSendException.class},
      maxAttempts = 3,
      backoff = @Backoff(delay = 2000))
  public void sendStockAvailabilityEmail(String to, String productName, Integer stock) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setTo(to);
    message.setSubject("Stock Availability for " + productName + "!");
    String body =
        String.format(
            "Good news! Product '%s' is now available.\n\n"
                + "We currently have %d units in stock.\n"
                + "Hurry before they're gone!",
            productName, stock);

    message.setText(body);
    mailSender.send(message);
  }
}

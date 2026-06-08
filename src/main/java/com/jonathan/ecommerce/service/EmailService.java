package com.jonathan.ecommerce.service;

import com.jonathan.ecommerce.dto.event.OrderPlacedEvent;
import jakarta.mail.MessagingException;
import java.time.Instant;

public interface EmailService {
  void sendStockAvailabilityEmail(String to, String userName, String productName, Integer stock)
      throws MessagingException;

  void sendAdminAlert(String subject, String message);

  void sendRegisterEmail(String to, String name, Instant date) throws MessagingException;

  void sendResetPasswordEmail(String to, String userName, String resetLink)
      throws MessagingException;

  void sendOrderConfirmationEmail(OrderPlacedEvent event) throws MessagingException;
}

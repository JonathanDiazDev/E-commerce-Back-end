package com.jonathan.ecommerce.kafka.consumer;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.jonathan.ecommerce.dto.event.PaymentRetryEvent;
import com.jonathan.ecommerce.entity.FailedPaymentEvent;
import com.jonathan.ecommerce.repository.FailedPaymentEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRetryDlqConsumerTest {

  @Mock private FailedPaymentEventRepository failedPaymentEventRepository;
  @Mock private EmailService emailService;

  @InjectMocks private PaymentRetryDlqConsumer paymentRetryDlqConsumer;

  @Test
  void handleDlq_Success() {
    PaymentRetryEvent event = new PaymentRetryEvent(1L, "pm_card_visa", 3, "msg-1", Instant.now());

    paymentRetryDlqConsumer.handleDlq(event, "Card declined");

    verify(failedPaymentEventRepository).save(any(FailedPaymentEvent.class));
    verify(emailService).sendAdminAlert(contains("Order #1"), contains("Card declined"));
  }

  @Test
  void handleDlq_NullExceptionMessage() {
    PaymentRetryEvent event = new PaymentRetryEvent(1L, "pm_card_visa", 3, "msg-1", Instant.now());

    paymentRetryDlqConsumer.handleDlq(event, null);

    verify(failedPaymentEventRepository).save(any(FailedPaymentEvent.class));
    verify(emailService).sendAdminAlert(anyString(), contains("Unknown error"));
  }
}

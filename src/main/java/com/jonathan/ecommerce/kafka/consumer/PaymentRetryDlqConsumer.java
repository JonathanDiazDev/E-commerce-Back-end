package com.jonathan.ecommerce.kafka.consumer;

import com.jonathan.ecommerce.dto.event.PaymentRetryEvent;
import com.jonathan.ecommerce.entity.FailedPaymentEvent;
import com.jonathan.ecommerce.kafka.config.KafkaConfig;
import com.jonathan.ecommerce.repository.FailedPaymentEventRepository;
import com.jonathan.ecommerce.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class PaymentRetryDlqConsumer {

  private final FailedPaymentEventRepository failedPaymentEventRepository;
  private final EmailService emailService;

  @KafkaListener(topics = KafkaConfig.PAYMENT_RETRY_TOPIC + ".DLT", groupId = "payment-dlq-group")
  public void handleDlq(
      PaymentRetryEvent event,
      @Header(name = KafkaHeaders.DLT_EXCEPTION_MESSAGE, required = false)
          String exceptionMessage) {

    log.error(
        "⚠️ Payment retry FAILED after 6 attempts for order {}. Exception: {}",
        event.orderId(),
        exceptionMessage != null ? exceptionMessage : "Unknown error");

    FailedPaymentEvent failedPaymentEvent = new FailedPaymentEvent();
    failedPaymentEvent.setOrderId(event.orderId());
    failedPaymentEvent.setErrorMessage(exceptionMessage);
    failedPaymentEvent.setAttemptNumber(event.attemptNumber());

    failedPaymentEventRepository.save(failedPaymentEvent);

    String subject = "❌ Payment retry Failed - Order #" + event.orderId();
    String message =
        "Order "
            + event.orderId()
            + " failed after 6 payment retry attempts.\n"
            + "Error: "
            + exceptionMessage
            + "\n"
            + "Please investigate manually.";

    emailService.sendAdminAlert(subject, message);
  }
}

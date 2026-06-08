package com.jonathan.ecommerce.kafka.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.service.EmailService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailRetryScheduler {

  private final FailedEmailRepository failedEmailRepository;
  private final ObjectMapper objectMapper;
  private final EmailService emailService;

  @Scheduled(fixedDelay = 120000)
  @Transactional
  public void retryFailedEmails() {

    List<FailedEmail> pendingEmails = failedEmailRepository.findTop50ByStatus(EmailStatus.PENDING);

    for (FailedEmail email : pendingEmails) {
      try {

        EmailRequest request = objectMapper.readValue(email.getPayload(), EmailRequest.class);

        Object stockObj = request.properties().get("totalStock");

        if (!(stockObj instanceof Number stockNumber)) {
          throw new IllegalArgumentException("Invalid stock value");
        }

        emailService.sendStockAvailabilityEmail(
            request.to(), request.userName(), request.productName(), stockNumber.intValue());

        email.setStatus(EmailStatus.SENT);

      } catch (Exception e) {

        email.setRetryCount(email.getRetryCount() + 1);

        if (email.getRetryCount() >= 5) {
          email.setStatus(EmailStatus.FAILED);
        }

        email.setErrorMessage(e.getMessage());
      }
    }
  }
}

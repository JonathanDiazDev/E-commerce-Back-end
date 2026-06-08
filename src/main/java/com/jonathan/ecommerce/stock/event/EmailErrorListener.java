package com.jonathan.ecommerce.stock.event;

import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailErrorListener {

  private final FailedEmailRepository failedEmailRepository;

  @EventListener
  public void handleEmailFailure(FailedEmailDTO event) {
    log.info("Saving error log for: {}", event.recipient());
    FailedEmail failedEmail =
        failedEmailRepository
            .findByRecipientAndEmailType(event.recipient(), event.emailType())
            .map(
                existing -> {
                  existing.setErrorMessage(event.errorMessage());
                  existing.setOccurredAt(event.occurredAt());
                  existing.setRetryCount(existing.getRetryCount() + 1);
                  return existing;
                })
            .orElseGet(
                () -> {
                  return FailedEmail.builder()
                      .recipient(event.recipient())
                      .emailType(event.emailType())
                      .payload(event.payload())
                      .errorMessage(event.errorMessage())
                      .occurredAt(event.occurredAt())
                      .retryCount(1)
                      .build();
                });
    failedEmailRepository.save(failedEmail);
  }
}

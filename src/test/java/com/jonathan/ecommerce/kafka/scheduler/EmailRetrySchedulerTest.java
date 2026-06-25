package com.jonathan.ecommerce.kafka.scheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jonathan.ecommerce.dto.request.EmailRequest;
import com.jonathan.ecommerce.entity.FailedEmail;
import com.jonathan.ecommerce.entity.enums.EmailStatus;
import com.jonathan.ecommerce.repository.FailedEmailRepository;
import com.jonathan.ecommerce.service.EmailService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailRetrySchedulerTest {

  @Mock private FailedEmailRepository failedEmailRepository;
  @Mock private ObjectMapper objectMapper;
  @Mock private EmailService emailService;

  @InjectMocks private EmailRetryScheduler emailRetryScheduler;

  @Test
  void retryFailedEmails_Success() throws Exception {
    FailedEmail failedEmail = new FailedEmail();
    failedEmail.setId(1L);
    failedEmail.setPayload("{}");
    failedEmail.setRetryCount(0);
    failedEmail.setStatus(EmailStatus.PENDING);

    EmailRequest emailRequest =
        new EmailRequest(
            "test@test.com",
            "User",
            "Product",
            "STOCK_AVAILABILITY",
            "msg-1",
            Map.of("totalStock", 10));

    when(failedEmailRepository.findTop50ByStatus(EmailStatus.PENDING))
        .thenReturn(List.of(failedEmail));
    when(objectMapper.readValue("{}", EmailRequest.class)).thenReturn(emailRequest);

    emailRetryScheduler.retryFailedEmails();

    verify(emailService).sendStockAvailabilityEmail("test@test.com", "User", "Product", 10);
    assertThat(failedEmail.getStatus()).isEqualTo(EmailStatus.SENT);
  }

  @Test
  void retryFailedEmails_InvalidStockValue() throws Exception {
    FailedEmail failedEmail = new FailedEmail();
    failedEmail.setId(1L);
    failedEmail.setPayload("{}");
    failedEmail.setRetryCount(0);
    failedEmail.setStatus(EmailStatus.PENDING);

    EmailRequest emailRequest =
        new EmailRequest(
            "test@test.com",
            "User",
            "Product",
            "STOCK_AVAILABILITY",
            "msg-1",
            Map.of("totalStock", "invalid"));

    when(failedEmailRepository.findTop50ByStatus(EmailStatus.PENDING))
        .thenReturn(List.of(failedEmail));
    when(objectMapper.readValue("{}", EmailRequest.class)).thenReturn(emailRequest);

    emailRetryScheduler.retryFailedEmails();

    verify(emailService, never())
        .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());
    assertThat(failedEmail.getStatus()).isNotEqualTo(EmailStatus.SENT);
  }

  @Test
  void retryFailedEmails_MaxRetriesReached() throws Exception {
    FailedEmail failedEmail = new FailedEmail();
    failedEmail.setId(1L);
    failedEmail.setPayload("{}");
    failedEmail.setRetryCount(5);
    failedEmail.setStatus(EmailStatus.PENDING);

    EmailRequest emailRequest =
        new EmailRequest(
            "test@test.com",
            "User",
            "Product",
            "STOCK_AVAILABILITY",
            "msg-1",
            Map.of("totalStock", 10));

    when(failedEmailRepository.findTop50ByStatus(EmailStatus.PENDING))
        .thenReturn(List.of(failedEmail));
    when(objectMapper.readValue("{}", EmailRequest.class)).thenReturn(emailRequest);
    doThrow(new RuntimeException("Email failed"))
        .when(emailService)
        .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());

    emailRetryScheduler.retryFailedEmails();

    assertThat(failedEmail.getRetryCount()).isEqualTo(6);
    assertThat(failedEmail.getStatus()).isEqualTo(EmailStatus.FAILED);
  }

  @Test
  void retryFailedEmails_NoPendingEmails() throws Exception {
    when(failedEmailRepository.findTop50ByStatus(EmailStatus.PENDING)).thenReturn(List.of());

    emailRetryScheduler.retryFailedEmails();

    verify(emailService, never())
        .sendStockAvailabilityEmail(anyString(), anyString(), anyString(), anyInt());
  }
}

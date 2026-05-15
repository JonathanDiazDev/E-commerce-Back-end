package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.entity.enums.EmailType;
import com.jonathan.ecommerce.stock.event.FailedEmailDTO;
import java.time.Instant;
import java.util.concurrent.Executor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.MailSendException;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
@EnableAsync
@RequiredArgsConstructor
@Slf4j
public class AsyncConfig implements AsyncConfigurer {

  private final ApplicationEventPublisher publisher;

  @Override
  public Executor getAsyncExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(2);
    executor.setMaxPoolSize(5);
    executor.setQueueCapacity(500);
    executor.setThreadNamePrefix("Email-Async-");
    executor.initialize();
    return executor;
  }

  @Override
  public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
    return (ex, method, params) -> {
      String emailDestino = "Unknown";
      String productName = "Unknown";
      if (params != null && params.length >= 2) {
        emailDestino = String.valueOf(params[0]);
        productName = String.valueOf(params[1]);
      }

      log.error("--- 🚨 ASYNCHRONOUS FAILURE REPORT ---");
      log.error("Affected method: {}", method.getName());
      log.error("Failed recipient: {}", emailDestino);
      log.error("Cause of the error: {}", ex.getMessage());

      if (ex instanceof MailSendException) {
        log.warn("Suggestion: Check Mailtrap quota or SMTP connection.");
      }
      log.error("------------------------------------");

      if (!emailDestino.equals("Unknown")) {
        FailedEmailDTO dto =
            new FailedEmailDTO(
                emailDestino, EmailType.UNKNOWN, "{}", ex.getMessage(), Instant.now());
        publisher.publishEvent(dto);
      }
    };
  }
}

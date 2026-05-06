package com.jonathan.ecommerce;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableRetry
@EnableAsync
@SpringBootApplication
public class EcommerceBackendApplication {
    @Value("${MAIL_USERNAME:NO_CARGADO}")
    private String mailUsername;

  public static void main(String[] args) {
    SpringApplication.run(EcommerceBackendApplication.class, args);
  }

  @jakarta.annotation.PostConstruct
    public void init() {
    System.out.println("MAIL_USERNAME: " + mailUsername);
  }
}

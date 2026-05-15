package com.jonathan.ecommerce;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;

@EnableRetry
@EnableAsync
@SpringBootApplication
public class EcommerceBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(EcommerceBackendApplication.class, args);
  }
}

package com.jonathan.ecommerce.config;

import com.jonathan.ecommerce.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.data.redis.host")
@Slf4j
public class DatabaseWarmup implements CommandLineRunner {

  private final UserRepository userRepository;
  private final RedisTemplate<String, Object> redisTemplate;

  @Override
  public void run(String... args) {
    log.info("Starting infrastructure warm-up... \uD83D\uDD27");

    try {
      userRepository.count();

      redisTemplate.hasKey("warmup-key");

      log.info("Infrastructure ready and hot connections! \uD83D\uDD25");
    } catch (Exception e) {
      log.error("Error during warm-up: {}", e.getMessage());
    }
  }
}

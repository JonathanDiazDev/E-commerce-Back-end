package com.jonathan.ecommerce.ratelimiting.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager; // Importa esta interfaz
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.api.StatefulRedisConnection;
import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@Slf4j
public class BucketConfig {

  @Bean
  public ProxyManager<byte[]> proxyManager(
      StatefulRedisConnection<byte[], byte[]> redisConnection) {
    log.info("Initializing Bucket4j ProxyManager with Redis backend");

    ProxyManager<byte[]> manager =
        LettuceBasedProxyManager.builderFor(redisConnection)
            // Buckets expire 1 hour after last write to prevent memory leaks
            .withExpirationStrategy(
                ExpirationAfterWriteStrategy.fixedTimeToLive(Duration.ofHours(1)))
            .build();

    log.info("Bucket4j ProxyManager initialized successfully");
    return manager;
  }
}

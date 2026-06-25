package com.jonathan.ecommerce.config;

import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.codec.ByteArrayCodec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@Profile("!test")
public class RedisConfig {

  @Bean
  public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
    RedisTemplate<String, Object> template = new RedisTemplate<>();
    StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();

    GenericJackson2JsonRedisSerializer genericJackson2JsonRedisSerializer =
        new GenericJackson2JsonRedisSerializer();
    template.setHashKeySerializer(stringRedisSerializer);
    template.setKeySerializer(stringRedisSerializer);
    template.setValueSerializer(genericJackson2JsonRedisSerializer);
    template.setHashValueSerializer(genericJackson2JsonRedisSerializer);
    template.setConnectionFactory(factory);
    template.afterPropertiesSet();

    return template;
  }

  @Value("${spring.data.redis.port}")
  private int redisPort;

  @Value("${spring.data.redis.host}")
  private String redisHost;

  @Bean
  public RedisClient redisClient() {
    return RedisClient.create("redis://" + redisHost + ":" + redisPort);
  }

  @Bean
  public StatefulRedisConnection<byte[], byte[]> redisConnection(RedisClient redisClient) {
    return redisClient.connect(new ByteArrayCodec());
  }
}

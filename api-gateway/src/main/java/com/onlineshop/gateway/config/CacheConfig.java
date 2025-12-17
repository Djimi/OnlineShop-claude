package com.onlineshop.gateway.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.onlineshop.gateway.dto.ValidateResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@Configuration
public class CacheConfig {

    @Value("${gateway.cache.caffeine.ttl-seconds:60}")
    private long caffeineTtlSeconds;

    @Value("${gateway.cache.caffeine.max-size:10000}")
    private long caffeineMaxSize;

    /**
     * L1 Cache - Caffeine (local in-memory)
     * Provides nanosecond access for frequently accessed tokens.
     */
    @Bean
    public Cache<String, ValidateResponse> tokenCache() {
        return Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofSeconds(caffeineTtlSeconds))
                .maximumSize(caffeineMaxSize)
                .recordStats()
                .build();
    }

    /**
     * L2 Cache - Redis template for distributed caching.
     * Provides shared cache across multiple gateway instances.
     */
    @Bean
    public RedisTemplate<String, ValidateResponse> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ValidateResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new Jackson2JsonRedisSerializer<>(ValidateResponse.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new Jackson2JsonRedisSerializer<>(ValidateResponse.class));
        template.afterPropertiesSet();
        return template;
    }
}

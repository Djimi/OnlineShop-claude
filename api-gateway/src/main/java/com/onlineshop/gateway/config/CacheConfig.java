package com.onlineshop.gateway.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.onlineshop.gateway.cache.TieredCacheManager;
import com.onlineshop.gateway.dto.ValidateResponse;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.cache.CaffeineCacheMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

/**
 * Cache configuration for multi-layer caching.
 *
 * <p>Provides two caching mechanisms:</p>
 * <ul>
 *   <li><b>Spring Cache Abstraction</b> (new): Used by {@code AuthValidationService} via
 *       {@code @Cacheable} annotation. Uses {@link TieredCacheManager} for L1→L2 hierarchical caching.</li>
 *   <li><b>Manual Caching</b> (legacy): Used by {@code AuthValidationServiceManual}.
 *       Direct Caffeine and Redis template access.</li>
 * </ul>
 *
 * <p>Metrics are automatically exposed via Micrometer with distinct cache names:</p>
 * <ul>
 *   <li>L1: cache_gets_total{cache="l1-auth-tokens", cacheManager="caffeineCacheManager"}</li>
 *   <li>L2: cache_gets_total{cache="l2-auth-tokens", cacheManager="redisCacheManager"}</li>
 * </ul>
 */
@Configuration
@EnableCaching
public class CacheConfig {

    @Value("${gateway.cache.caffeine.ttl-seconds:60}")
    private long caffeineTtlSeconds;

    @Value("${gateway.cache.caffeine.max-size:10000}")
    private long caffeineMaxSize;

    @Value("${gateway.cache.redis.ttl-seconds:300}")
    private long redisTtlSeconds;

    // ===== Spring Cache Abstraction Beans (for @Cacheable) =====

    /**
     * Primary CacheManager that provides tiered caching (L1 + L2).
     * Used by @Cacheable annotations in AuthValidationService.
     */
    @Bean
    @Primary
    public CacheManager cacheManager(
            CaffeineCacheManager caffeineCacheManager,
            RedisCacheManager redisCacheManager,
            CircuitBreaker redisCacheCircuitBreaker,
            io.micrometer.core.instrument.MeterRegistry meterRegistry) {
        return new TieredCacheManager(
                caffeineCacheManager,
                redisCacheManager,
                redisCacheCircuitBreaker,
                meterRegistry);
    }

    /**
     * L1 CacheManager using Caffeine (local in-memory).
     * Provides nanosecond access for frequently accessed tokens.
     */
    @Bean
    public CaffeineCacheManager caffeineCacheManager(MeterRegistry meterRegistry) {
        Caffeine<Object, Object> caffeineBuilder = Caffeine.newBuilder()
                .recordStats()  // Enables Micrometer metrics
                .expireAfterWrite(Duration.ofSeconds(caffeineTtlSeconds))
                .maximumSize(caffeineMaxSize);
        com.github.benmanes.caffeine.cache.Cache<Object, Object> nativeCache = caffeineBuilder.build();
        CaffeineCacheManager manager = new CaffeineCacheManager();
        manager.registerCustomCache("l1-auth-tokens", nativeCache);
        CaffeineCacheMetrics.monitor(meterRegistry, nativeCache, "l1-auth-tokens");
        return manager;
    }

    /**
     * L2 CacheManager using Redis (distributed).
     * Provides shared cache across multiple gateway instances.
     */
    @Bean
    public RedisCacheManager redisCacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(redisTtlSeconds))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new JacksonJsonRedisSerializer<>(ValidateResponse.class)));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(config)
                .enableStatistics()  // Enables Micrometer metrics
                .initialCacheNames(java.util.Set.of("l2-auth-tokens"))
                .build();
    }

    /**
     * Circuit breaker for Redis cache operations.
     * Protects against Redis failures and enables graceful degradation to L1-only mode.
     */
    @Bean
    public CircuitBreaker redisCacheCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker("redisCacheSpring");
    }

    // ===== Legacy Manual Caching Beans (for AuthValidationServiceManual) =====

    /**
     * L1 Cache - Caffeine (local in-memory).
     * Used by DefaultTokenCacheManagerManualImpl for legacy manual caching.
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
     * Used by DefaultTokenCacheManagerManualImpl for legacy manual caching.
     */
    @Bean
    public RedisTemplate<String, ValidateResponse> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, ValidateResponse> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(new JacksonJsonRedisSerializer<>(ValidateResponse.class));
        template.setHashKeySerializer(new StringRedisSerializer());
        template.setHashValueSerializer(new JacksonJsonRedisSerializer<>(ValidateResponse.class));
        template.afterPropertiesSet();
        return template;
    }
}

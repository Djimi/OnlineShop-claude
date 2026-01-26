package com.onlineshop.gateway.cache;

import com.github.benmanes.caffeine.cache.Cache;
import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.metrics.GatewayMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

//@Component
@Slf4j
public class DefaultTokenCacheManagerManualImpl implements TokenCacheManager {

    private final Cache<String, ValidateResponse> caffeineCache;
    private final RedisTemplate<String, ValidateResponse> redisTemplate;
    private final long redisTtlSeconds;
    private final CircuitBreaker redisCircuitBreaker;
    private final GatewayMetrics metrics;

    private static final String REDIS_KEY_PREFIX = "token:";

    public DefaultTokenCacheManagerManualImpl(
            Cache<String, ValidateResponse> caffeineCache,
            RedisTemplate<String, ValidateResponse> redisTemplate,
            GatewayMetrics metrics,
            @Value("${gateway.cache.redis.ttl-seconds:300}") long redisTtlSeconds) {
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
        this.redisTtlSeconds = redisTtlSeconds;

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        this.redisCircuitBreaker = registry.circuitBreaker("redisCache");
    }

    @Override
    public Optional<ValidateResponse> get(String token) {
        String tokenHash = hashToken(token);

        // L1: Check Caffeine cache first (nanosecond access)
        ValidateResponse cachedResponse = caffeineCache.getIfPresent(tokenHash);
        if (cachedResponse != null) {
            log.debug("Token validation hit L1 cache (Caffeine)");
            metrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);
            // Check expiry before returning
            if (isExpired(cachedResponse)) {
                log.debug("L1 cached token expired, evicting");
                caffeineCache.invalidate(tokenHash);
                return Optional.empty();
            }
            return cachedResponse.isValid() ? Optional.of(cachedResponse) : Optional.empty();
        }
        metrics.recordCacheMiss(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);

        // L2: Check Redis cache (millisecond access, shared across instances)
        try {
            ValidateResponse redisResponse = redisCircuitBreaker.executeSupplier(() ->
                    redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + tokenHash)
            );

            if (redisResponse != null) {
                log.debug("Token validation hit L2 cache (Redis)");
                metrics.recordCacheHit(GatewayMetrics.LAYER_L2, GatewayMetrics.SERVICE_AUTH);
                // Check expiry before returning
                if (isExpired(redisResponse)) {
                    log.debug("L2 cached token expired, evicting");
                    try {
                        redisCircuitBreaker.executeRunnable(() ->
                                redisTemplate.delete(REDIS_KEY_PREFIX + tokenHash)
                        );
                    } catch (Exception e) {
                        log.warn("Failed to delete expired token from Redis: {}", e.getMessage());
                    }
                    return Optional.empty();
                }
                // Promote to L1 cache
                caffeineCache.put(tokenHash, redisResponse);
                return redisResponse.isValid() ? Optional.of(redisResponse) : Optional.empty();
            }
            metrics.recordCacheMiss(GatewayMetrics.LAYER_L2, GatewayMetrics.SERVICE_AUTH);
        } catch (Exception e) {
            log.warn("Redis cache lookup failed, continuing without cache: {}", e.getMessage());
            metrics.recordCacheMiss(GatewayMetrics.LAYER_L2, GatewayMetrics.SERVICE_AUTH);
        }

        return Optional.empty();
    }

    @Override
    public void put(String token, ValidateResponse response) {
        String tokenHash = hashToken(token);

        // Store in L1 cache
        caffeineCache.put(tokenHash, response);

        // Store in L2 cache (Redis)
        try {
            redisCircuitBreaker.executeRunnable(() ->
                    redisTemplate.opsForValue().set(
                            REDIS_KEY_PREFIX + tokenHash,
                            response,
                            Duration.ofSeconds(redisTtlSeconds)
                    )
            );
        } catch (Exception e) {
            log.warn("Failed to store token in Redis cache: {}", e.getMessage());
        }
    }

    /**
     * Hashes the token using SHA-256 for secure cache storage.
     * This is an internal implementation detail.
     *
     * @param token the raw token
     * @return the hashed token
     */
    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }

    private boolean isExpired(ValidateResponse response) {
        if (response.getExpiresAt() == null) {
            return false;
        }
        // Add 30-second grace period to handle clock skew
        Instant expiryWithGrace = response.getExpiresAt().minusSeconds(30);
        return Instant.now().isAfter(expiryWithGrace);
    }
}

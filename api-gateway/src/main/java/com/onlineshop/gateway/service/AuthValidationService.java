package com.onlineshop.gateway.service;

import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.metrics.GatewayMetrics;
import com.onlineshop.gateway.validation.TokenSanitizer;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Token validation service using Spring Cache Abstraction.
 * Uses a TieredCacheManager for multi-layer caching (L1: Caffeine, L2: Redis).
 *
 * <p>The caching is handled declaratively via {@code @Cacheable} annotation.
 * Spring automatically:</p>
 * <ul>
 *   <li>Checks the cache before method execution</li>
 *   <li>Returns cached value if found (method never executes)</li>
 *   <li>Calls the method on cache miss and stores the result</li>
 * </ul>
 *
 * <p>The cache key is the SHA-256 hash of the token to avoid storing raw tokens.</p>
 */
@Service
@Slf4j
public class AuthValidationService implements TokenValidator {

    private final AuthServiceClient authServiceClient;
    private final TokenSanitizer tokenSanitizer;
    private final GatewayMetrics metrics;

    public AuthValidationService(
            AuthServiceClient authServiceClient,
            TokenSanitizer tokenSanitizer,
            GatewayMetrics metrics) {
        this.authServiceClient = authServiceClient;
        this.tokenSanitizer = tokenSanitizer;
        this.metrics = metrics;
    }

    /**
     * Validates a token using multi-layer caching via Spring Cache Abstraction.
     *
     * <p>Cache flow:</p>
     * <ol>
     *   <li>Check L1 (Caffeine) - nanosecond access</li>
     *   <li>Check L2 (Redis) - millisecond access, with L1 promotion on hit</li>
     *   <li>Call Auth Service on cache miss, store result in both caches</li>
     * </ol>
     *
     * @param token the authentication token to validate
     * @return ValidateResponse containing user information if valid
     * @throws com.onlineshop.gateway.exception.InvalidTokenFormatException if token format is invalid
     * @throws com.onlineshop.gateway.exception.ServiceUnavailableException if Auth service is unavailable
     */
    @Override
    @Cacheable(
            cacheNames = "auth-tokens",
            key = "T(com.onlineshop.gateway.util.TokenHasher).hash(#token)",
            unless = "#result == null || !#result.valid"
    )
    public ValidateResponse validateToken(String token) {
        // Validate token format first - throws InvalidTokenFormatException if invalid
        tokenSanitizer.validate(token);

        // This code only runs on cache miss
        log.debug("Token validation cache miss, calling Auth service");
        Timer.Sample sample = metrics.startAuthServiceTimer();
        try {
            ValidateResponse authResponse = authServiceClient.validateToken(token).join();
            log.debug("Auth service response received, valid={}", authResponse.isValid());
            return authResponse;
        } finally {
            metrics.stopAuthServiceTimer(sample);
        }
    }
}

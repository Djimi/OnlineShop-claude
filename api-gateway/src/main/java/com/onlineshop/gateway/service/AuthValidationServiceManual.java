package com.onlineshop.gateway.service;

import com.onlineshop.gateway.cache.TokenCacheManager;
import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.exception.InvalidTokenFormatException;
import com.onlineshop.gateway.metrics.GatewayMetrics;
import com.onlineshop.gateway.validation.TokenSanitizer;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional; // Still needed for cache.get() return type

//@Service
@Slf4j
@Deprecated
public class AuthValidationServiceManual implements TokenValidator {

    private final TokenCacheManager cacheManager;
    private final AuthServiceClient authServiceClient;
    private final TokenSanitizer tokenSanitizer;
    private final GatewayMetrics metrics;

    public AuthValidationServiceManual(
            TokenCacheManager cacheManager,
            AuthServiceClient authServiceClient,
            TokenSanitizer tokenSanitizer,
            GatewayMetrics metrics) {
        this.cacheManager = cacheManager;
        this.authServiceClient = authServiceClient;
        this.tokenSanitizer = tokenSanitizer;
        this.metrics = metrics;
    }

    /**
     * Validates a token using multi-layer caching:
     * L1 (Caffeine) -> L2 (Redis) -> Auth Service
     *
     * @throws InvalidTokenFormatException if token format is invalid
     */
    @Override
    public ValidateResponse validateToken(String token) {
        // Validate token format first - throws InvalidTokenFormatException if invalid
        tokenSanitizer.validate(token);

        // Check cache first
        Optional<ValidateResponse> cachedResponse = cacheManager.get(token);
        if (cachedResponse.isPresent()) {
            return cachedResponse.get();
        }

        // Cache miss - call Auth service
        log.debug("Token validation cache miss, calling Auth service");
        Timer.Sample sample = metrics.startAuthServiceTimer();
        try {
            ValidateResponse authResponse = authServiceClient.validateToken(token).join();

            // Cache valid response
            cacheManager.put(token, authResponse);
            return authResponse;
        } finally {
            metrics.stopAuthServiceTimer(sample);
        }
    }
}

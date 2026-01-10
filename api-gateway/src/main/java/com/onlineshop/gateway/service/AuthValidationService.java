package com.onlineshop.gateway.service;

import com.onlineshop.gateway.cache.TokenCacheManager;
import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.metrics.GatewayMetrics;
import com.onlineshop.gateway.validation.TokenSanitizer;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class AuthValidationService implements TokenValidator {

    private final TokenCacheManager cacheManager;
    private final AuthServiceClient authServiceClient;
    private final TokenSanitizer tokenSanitizer;
    private final GatewayMetrics metrics;

    public AuthValidationService(
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
     */
    @Override
    public Optional<ValidateResponse> validateToken(String token) {
        // Validate token format first
        if (!tokenSanitizer.isValid(token)) {
            log.debug("Token validation failed: invalid token format");
            return Optional.empty();
        }

        String tokenHash = cacheManager.hashToken(token);

        // Check cache first
        Optional<ValidateResponse> cachedResponse = cacheManager.get(tokenHash);
        if (cachedResponse.isPresent()) {
            return cachedResponse;
        }

        // Cache miss - call Auth service
        log.debug("Token validation cache miss, calling Auth service");
        Timer.Sample sample = metrics.startAuthServiceTimer();
        try {
            Optional<ValidateResponse> authResponse = authServiceClient.validateToken(token);

            if (authResponse.isPresent()) {
                // Cache valid response
                cacheManager.put(tokenHash, authResponse.get());
                return authResponse;
            } else {
                // Cache invalid response briefly to prevent hammering Auth service
                ValidateResponse invalidResponse = ValidateResponse.invalid();
                cacheManager.put(tokenHash, invalidResponse);
                return Optional.empty();
            }
        } finally {
            metrics.stopAuthServiceTimer(sample);
        }
    }
}

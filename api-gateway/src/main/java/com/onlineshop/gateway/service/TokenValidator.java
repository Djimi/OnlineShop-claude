package com.onlineshop.gateway.service;

import com.onlineshop.gateway.dto.ValidateResponse;

import java.util.Optional;

/**
 * Interface for token validation logic.
 * Supports multi-layer caching (L1 Caffeine, L2 Redis, L3 Auth Service).
 */
public interface TokenValidator {

    /**
     * Validates a token using multi-layer caching.
     *
     * @param token the authentication token to validate
     * @return ValidateResponse if token is valid, empty otherwise
     */
    Optional<ValidateResponse> validateToken(String token);
}

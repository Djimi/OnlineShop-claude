package com.onlineshop.gateway.cache;

import com.onlineshop.gateway.dto.ValidateResponse;

import java.util.Optional;

/**
 * Interface for managing token cache operations across L1 (Caffeine) and L2 (Redis) layers.
 * The cache manager handles token hashing internally for secure storage.
 */
public interface TokenCacheManager {

    /**
     * Gets a token from cache (checks L1 first, then L2).
     * The token is hashed internally before lookup.
     *
     * @param token the raw token
     * @return cached ValidateResponse if found, empty otherwise
     */
    Optional<ValidateResponse> get(String token);

    /**
     * Stores a token in both L1 and L2 caches.
     * The token is hashed internally before storage.
     *
     * @param token the raw token
     * @param response the validation response to cache
     */
    void put(String token, ValidateResponse response);
}

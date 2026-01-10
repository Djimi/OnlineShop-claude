package com.onlineshop.gateway.cache;

import com.onlineshop.gateway.dto.ValidateResponse;

import java.util.Optional;

/**
 * Interface for managing token cache operations across L1 (Caffeine) and L2 (Redis) layers.
 */
public interface TokenCacheManager {

    /**
     * Gets a token from cache (checks L1 first, then L2).
     *
     * @param tokenHash the hashed token
     * @return cached ValidateResponse if found, empty otherwise
     */
    Optional<ValidateResponse> get(String tokenHash);

    /**
     * Stores a token in both L1 and L2 caches.
     *
     * @param tokenHash the hashed token
     * @param response the validation response to cache
     */
    void put(String tokenHash, ValidateResponse response);

    /**
     * Hashes the token using SHA-256 for secure cache storage.
     *
     * @param token the plain token
     * @return the hashed token
     */
    String hashToken(String token);
}

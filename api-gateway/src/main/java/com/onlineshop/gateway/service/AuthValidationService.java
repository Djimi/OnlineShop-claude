package com.onlineshop.gateway.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.onlineshop.gateway.dto.ValidateResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Optional;

@Service
@Slf4j
public class AuthValidationService {

    private final Cache<String, ValidateResponse> caffeineCache;
    private final RedisTemplate<String, ValidateResponse> redisTemplate;
    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String validateEndpoint;
    private final long redisTtlSeconds;

    private static final String REDIS_KEY_PREFIX = "token:";

    public AuthValidationService(
            Cache<String, ValidateResponse> caffeineCache,
            RedisTemplate<String, ValidateResponse> redisTemplate,
            @Value("${gateway.auth.service-url}") String authServiceUrl,
            @Value("${gateway.auth.validate-endpoint}") String validateEndpoint,
            @Value("${gateway.cache.redis.ttl-seconds:300}") long redisTtlSeconds) {
        this.caffeineCache = caffeineCache;
        this.redisTemplate = redisTemplate;
        this.restTemplate = new RestTemplate();
        this.authServiceUrl = authServiceUrl;
        this.validateEndpoint = validateEndpoint;
        this.redisTtlSeconds = redisTtlSeconds;
    }

    /**
     * Validates a token using multi-layer caching:
     * L1 (Caffeine) -> L2 (Redis) -> Auth Service
     */
    public Optional<ValidateResponse> validateToken(String token) {
        String tokenHash = hashToken(token);

        // L1: Check Caffeine cache first (nanosecond access)
        ValidateResponse cachedResponse = caffeineCache.getIfPresent(tokenHash);
        if (cachedResponse != null) {
            log.debug("Token validation hit L1 cache (Caffeine)");
            return cachedResponse.isValid() ? Optional.of(cachedResponse) : Optional.empty();
        }

        // L2: Check Redis cache (millisecond access, shared across instances)
        try {
            ValidateResponse redisResponse = redisTemplate.opsForValue().get(REDIS_KEY_PREFIX + tokenHash);
            if (redisResponse != null) {
                log.debug("Token validation hit L2 cache (Redis)");
                // Promote to L1 cache
                caffeineCache.put(tokenHash, redisResponse);
                return redisResponse.isValid() ? Optional.of(redisResponse) : Optional.empty();
            }
        } catch (Exception e) {
            log.warn("Redis cache lookup failed, falling back to Auth service: {}", e.getMessage());
        }

        // L3: Call Auth service
        log.debug("Token validation cache miss, calling Auth service");
        return callAuthService(token, tokenHash);
    }

    private Optional<ValidateResponse> callAuthService(String token, String tokenHash) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer: " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ValidateResponse> response = restTemplate.exchange(
                    authServiceUrl + validateEndpoint,
                    HttpMethod.GET,
                    entity,
                    ValidateResponse.class
            );

            ValidateResponse validateResponse = response.getBody();
            if (validateResponse != null && validateResponse.isValid()) {
                // Store in both caches
                caffeineCache.put(tokenHash, validateResponse);
                try {
                    redisTemplate.opsForValue().set(
                            REDIS_KEY_PREFIX + tokenHash,
                            validateResponse,
                            Duration.ofSeconds(redisTtlSeconds)
                    );
                } catch (Exception e) {
                    log.warn("Failed to store token in Redis cache: {}", e.getMessage());
                }
                return Optional.of(validateResponse);
            }

            // Cache invalid response briefly to prevent hammering Auth service
            ValidateResponse invalidResponse = ValidateResponse.invalid();
            caffeineCache.put(tokenHash, invalidResponse);

            return Optional.empty();

        } catch (RestClientException e) {
            log.error("Failed to validate token with Auth service: {}", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Hashes the token using SHA-256 for secure cache storage.
     * Never store plain tokens in cache.
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
}

package com.onlineshop.gateway.cache;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A tiered CacheManager that combines L1 (Caffeine) and L2 (Redis) caches.
 * Each cache request creates a TieredCache that wraps both layers.
 *
 * <p>Internal cache names are prefixed with "l1-" and "l2-" to provide
 * distinct metrics for each layer via Micrometer:</p>
 * <ul>
 *   <li>L1 metrics: cache_gets_total{cache="l1-auth-tokens", cacheManager="caffeineCacheManager"}</li>
 *   <li>L2 metrics: cache_gets_total{cache="l2-auth-tokens", cacheManager="redisCacheManager"}</li>
 * </ul>
 */
@Slf4j
public class TieredCacheManager implements CacheManager {

    private static final String L1_PREFIX = "l1-";
    private static final String L2_PREFIX = "l2-";

    private final CaffeineCacheManager l1CacheManager;
    private final RedisCacheManager l2CacheManager;
    private final CircuitBreaker circuitBreaker;
    private final Map<String, TieredCache> cacheMap = new ConcurrentHashMap<>();

    public TieredCacheManager(
            CaffeineCacheManager l1CacheManager,
            RedisCacheManager l2CacheManager,
            CircuitBreaker circuitBreaker) {
        this.l1CacheManager = l1CacheManager;
        this.l2CacheManager = l2CacheManager;
        this.circuitBreaker = circuitBreaker;
        log.info("TieredCacheManager initialized with L1 (Caffeine) and L2 (Redis) caches");
    }

    @Override
    public Cache getCache(String name) {
        return cacheMap.computeIfAbsent(name, this::createTieredCache);
    }

    private TieredCache createTieredCache(String name) {
        // Use distinct names for L1 and L2 to get separate metrics per layer
        String l1Name = L1_PREFIX + name;
        String l2Name = L2_PREFIX + name;

        Cache l1Cache = l1CacheManager.getCache(l1Name);
        Cache l2Cache = l2CacheManager.getCache(l2Name);

        if (l1Cache == null) {
            throw new IllegalStateException("Failed to create L1 cache for: " + l1Name);
        }
        if (l2Cache == null) {
            throw new IllegalStateException("Failed to create L2 cache for: " + l2Name);
        }

        log.debug("Created TieredCache '{}' with L1='{}' and L2='{}'", name, l1Name, l2Name);
        return new TieredCache(name, l1Cache, l2Cache, circuitBreaker);
    }

    @Override
    public Collection<String> getCacheNames() {
        return cacheMap.keySet();
    }
}

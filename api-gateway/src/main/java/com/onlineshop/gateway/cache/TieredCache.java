package com.onlineshop.gateway.cache;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * A tiered cache implementation that wraps L1 (Caffeine) and L2 (Redis) caches.
 * Provides automatic L1→L2 promotion: when L2 has a hit but L1 doesn't,
 * the value is promoted to L1 for faster subsequent access.
 *
 * <p>Redis operations are protected by a circuit breaker to handle Redis failures gracefully.
 * When Redis is unavailable, the cache degrades to L1-only mode.</p>
 *
 * <p>Metrics are automatically collected by the underlying CaffeineCacheManager and
 * RedisCacheManager when {@code recordStats()} and {@code enableStatistics()} are enabled.</p>
 */
@Slf4j
public class TieredCache implements Cache {

    private final String name;
    private final Cache l1Cache;
    private final Cache l2Cache;
    private final CircuitBreaker circuitBreaker;
    private final Executor l2WriteExecutor;

    public TieredCache(String name, Cache l1Cache, Cache l2Cache, CircuitBreaker circuitBreaker, Executor l2WriteExecutor) {
        this.name = name;
        this.l1Cache = l1Cache;
        this.l2Cache = l2Cache;
        this.circuitBreaker = circuitBreaker;
        this.l2WriteExecutor = l2WriteExecutor;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getNativeCache() {
        return this;
    }

    @Override
    public ValueWrapper get(Object key) {
        // 1. Check L1 (Caffeine) - always available, nanosecond access
        ValueWrapper l1Result = l1Cache.get(key);
        if (l1Result != null) {
            log.debug("Cache hit on L1 for key hash: {}", key);
            return l1Result;
        }

        // 2. Check L2 (Redis) with circuit breaker protection
        try {
            ValueWrapper l2Result = circuitBreaker.executeSupplier(() -> l2Cache.get(key));
            if (l2Result != null) {
                log.debug("Cache hit on L2 for key hash: {}, promoting to L1", key);
                // Promote to L1 for faster subsequent access
                l1Cache.put(key, l2Result.get());
                return l2Result;
            }
        } catch (Exception e) {
            log.warn("L2 cache (Redis) unavailable for GET: {}", e.getMessage());
            // Continue without L2 - graceful degradation
        }

        log.debug("Cache miss for key hash: {}", key);
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Class<T> type) {
        ValueWrapper wrapper = get(key);
        if (wrapper == null) {
            return null;
        }
        Object value = wrapper.get();
        if (value != null && type != null && !type.isInstance(value)) {
            throw new IllegalStateException(
                    "Cached value is not of required type [" + type.getName() + "]: " + value);
        }
        return (T) value;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T get(Object key, Callable<T> valueLoader) {
        ValueWrapper wrapper = get(key);
        if (wrapper != null) {
            return (T) wrapper.get();
        }

        // Cache miss - call the value loader
        try {
            T value = valueLoader.call();
            put(key, value);
            return value;
        } catch (Exception e) {
            throw new ValueRetrievalException(key, valueLoader, e);
        }
    }

    @Override
    public void put(Object key, Object value) {
        // Always write to L1 (Caffeine)
        l1Cache.put(key, value);

        // Write to L2 (Redis) asynchronously with circuit breaker protection
        circuitBreaker.executeCompletionStage(() ->
                CompletableFuture.runAsync(() -> l2Cache.put(key, value), l2WriteExecutor)
        ).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.debug("Stored in L2 for key hash: {}", key);
            } else {
                log.warn("Failed to write to L2 cache (Redis): {}", throwable.getMessage());
                // L1 write succeeded, so the cache operation is partially successful
            }
        });
    }

    @Override
    public void evict(Object key) {
        // Evict from L1
        l1Cache.evict(key);

        // Evict from L2 asynchronously with circuit breaker protection
        circuitBreaker.executeCompletionStage(() ->
                CompletableFuture.runAsync(() -> l2Cache.evict(key), l2WriteExecutor)
        ).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.debug("Evicted from L2 for key hash: {}", key);
            } else {
                log.warn("Failed to evict from L2 cache (Redis): {}", throwable.getMessage());
            }
        });
    }

    @Override
    public boolean evictIfPresent(Object key) {
        boolean l1Evicted = l1Cache.evictIfPresent(key);

        circuitBreaker.executeCompletionStage(() ->
                CompletableFuture.runAsync(() -> l2Cache.evictIfPresent(key), l2WriteExecutor)
        ).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.debug("EvictedIfPresent from L2 for key hash: {}", key);
            } else {
                log.warn("Failed to evictIfPresent from L2 cache (Redis): {}", throwable.getMessage());
            }
        });

        return l1Evicted;
    }

    @Override
    public void clear() {
        // Clear L1
        l1Cache.clear();

        // Clear L2 asynchronously with circuit breaker protection
        circuitBreaker.executeCompletionStage(() ->
                CompletableFuture.runAsync(l2Cache::clear, l2WriteExecutor)
        ).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.debug("Cleared L2 cache");
            } else {
                log.warn("Failed to clear L2 cache (Redis): {}", throwable.getMessage());
            }
        });
    }

    @Override
    public boolean invalidate() {
        boolean l1Invalidated = l1Cache.invalidate();

        circuitBreaker.executeCompletionStage(() ->
                CompletableFuture.runAsync(l2Cache::invalidate, l2WriteExecutor)
        ).whenComplete((ignored, throwable) -> {
            if (throwable == null) {
                log.debug("Invalidated L2 cache");
            } else {
                log.warn("Failed to invalidate L2 cache (Redis): {}", throwable.getMessage());
            }
        });

        return l1Invalidated;
    }
}

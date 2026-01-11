package com.onlineshop.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GatewayMetrics {

    // Constants to avoid string typos
    public static final String SERVICE_AUTH = "auth";
    public static final String SERVICE_ITEMS = "items";
    public static final String LAYER_L1 = "l1";
    public static final String LAYER_L2 = "l2";
    public static final String RESULT_HIT = "hit";
    public static final String RESULT_MISS = "miss";

    private final MeterRegistry meterRegistry;
    private final Timer authServiceLatency;
    private final Counter rateLimitRejections;

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        // Keep these as pre-registered (low cardinality, single instance)
        this.authServiceLatency = Timer.builder("gateway.auth.service.latency")
                .description("Auth service call latency")
                .register(meterRegistry);

        this.rateLimitRejections = Counter.builder("gateway.ratelimit.rejections.total")
                .description("Rate limit rejections")
                .register(meterRegistry);
    }

    /**
     * Record a cache operation result.
     *
     * @param layer "l1" (Caffeine) or "l2" (Redis)
     * @param service "auth", "items", etc.
     * @param result "hit" or "miss"
     */
    public void recordCacheOperation(String layer, String service, String result) {
        Counter.builder("gateway.cache.operations.total")
                .tag("layer", layer)
                .tag("service", service)
                .tag("result", result)
                .description("Cache operation results")
                .register(meterRegistry)
                .increment();
    }

    /**
     * Convenience method to record a cache hit.
     *
     * @param layer "l1" (Caffeine) or "l2" (Redis)
     * @param service "auth", "items", etc.
     */
    public void recordCacheHit(String layer, String service) {
        recordCacheOperation(layer, service, RESULT_HIT);
    }

    /**
     * Convenience method to record a cache miss.
     *
     * @param layer "l1" (Caffeine) or "l2" (Redis)
     * @param service "auth", "items", etc.
     */
    public void recordCacheMiss(String layer, String service) {
        recordCacheOperation(layer, service, RESULT_MISS);
    }

    public void incrementRateLimitRejections() {
        rateLimitRejections.increment();
    }

    public Timer.Sample startAuthServiceTimer() {
        return Timer.start();
    }

    public void stopAuthServiceTimer(Timer.Sample sample) {
        sample.stop(authServiceLatency);
    }
}

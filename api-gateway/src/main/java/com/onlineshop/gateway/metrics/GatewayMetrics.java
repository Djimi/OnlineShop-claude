package com.onlineshop.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class GatewayMetrics {

    private final Counter l1CacheHits;
    private final Counter l1CacheMisses;
    private final Counter l2CacheHits;
    private final Counter l2CacheMisses;
    private final Counter rateLimitRejections;
    private final Timer authServiceLatency;

    public GatewayMetrics(MeterRegistry meterRegistry) {
        this.l1CacheHits = Counter.builder("gateway.cache.l1.hits")
                .description("L1 (Caffeine) cache hits")
                .register(meterRegistry);

        this.l1CacheMisses = Counter.builder("gateway.cache.l1.misses")
                .description("L1 (Caffeine) cache misses")
                .register(meterRegistry);

        this.l2CacheHits = Counter.builder("gateway.cache.l2.hits")
                .description("L2 (Redis) cache hits")
                .register(meterRegistry);

        this.l2CacheMisses = Counter.builder("gateway.cache.l2.misses")
                .description("L2 (Redis) cache misses")
                .register(meterRegistry);

        this.rateLimitRejections = Counter.builder("gateway.ratelimit.rejections")
                .description("Rate limit rejections")
                .register(meterRegistry);

        this.authServiceLatency = Timer.builder("gateway.auth.service.latency")
                .description("Auth service call latency")
                .register(meterRegistry);
    }

    public void incrementL1CacheHits() {
        l1CacheHits.increment();
    }

    public void incrementL1CacheMisses() {
        l1CacheMisses.increment();
    }

    public void incrementL2CacheHits() {
        l2CacheHits.increment();
    }

    public void incrementL2CacheMisses() {
        l2CacheMisses.increment();
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

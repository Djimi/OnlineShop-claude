package com.onlineshop.gateway.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayMetricsTest {

    private MeterRegistry meterRegistry;
    private GatewayMetrics gatewayMetrics;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        gatewayMetrics = new GatewayMetrics(meterRegistry);
    }

    @Test
    void shouldRecordCacheHitWithCorrectTags() {
        // Given
        String layer = GatewayMetrics.LAYER_L1;
        String service = GatewayMetrics.SERVICE_AUTH;

        // When
        gatewayMetrics.recordCacheHit(layer, service);

        // Then
        Counter counter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", layer)
                .tag("service", service)
                .tag("result", GatewayMetrics.RESULT_HIT)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordCacheMissWithCorrectTags() {
        // Given
        String layer = GatewayMetrics.LAYER_L2;
        String service = GatewayMetrics.SERVICE_AUTH;

        // When
        gatewayMetrics.recordCacheMiss(layer, service);

        // Then
        Counter counter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", layer)
                .tag("service", service)
                .tag("result", GatewayMetrics.RESULT_MISS)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordMultipleCacheOperations() {
        // Given & When
        gatewayMetrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);
        gatewayMetrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);
        gatewayMetrics.recordCacheMiss(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);

        // Then
        Counter hitCounter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", GatewayMetrics.LAYER_L1)
                .tag("service", GatewayMetrics.SERVICE_AUTH)
                .tag("result", GatewayMetrics.RESULT_HIT)
                .counter();

        Counter missCounter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", GatewayMetrics.LAYER_L1)
                .tag("service", GatewayMetrics.SERVICE_AUTH)
                .tag("result", GatewayMetrics.RESULT_MISS)
                .counter();

        assertThat(hitCounter).isNotNull();
        assertThat(hitCounter.count()).isEqualTo(2.0);
        assertThat(missCounter).isNotNull();
        assertThat(missCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldRecordCacheOperationDirectlyWithAllTags() {
        // Given
        String layer = GatewayMetrics.LAYER_L2;
        String service = GatewayMetrics.SERVICE_ITEMS;
        String result = GatewayMetrics.RESULT_HIT;

        // When
        gatewayMetrics.recordCacheOperation(layer, service, result);

        // Then
        Counter counter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", layer)
                .tag("service", service)
                .tag("result", result)
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldSupportMultipleServices() {
        // Given & When - Simulate future where we have multiple services
        gatewayMetrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);
        gatewayMetrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_ITEMS);

        // Then - Each service should have its own counter
        Counter authCounter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", GatewayMetrics.LAYER_L1)
                .tag("service", GatewayMetrics.SERVICE_AUTH)
                .tag("result", GatewayMetrics.RESULT_HIT)
                .counter();

        Counter itemsCounter = meterRegistry.find("gateway.cache.operations.total")
                .tag("layer", GatewayMetrics.LAYER_L1)
                .tag("service", GatewayMetrics.SERVICE_ITEMS)
                .tag("result", GatewayMetrics.RESULT_HIT)
                .counter();

        assertThat(authCounter).isNotNull();
        assertThat(authCounter.count()).isEqualTo(1.0);
        assertThat(itemsCounter).isNotNull();
        assertThat(itemsCounter.count()).isEqualTo(1.0);
    }

    @Test
    void shouldIncrementRateLimitRejections() {
        // When
        gatewayMetrics.incrementRateLimitRejections();
        gatewayMetrics.incrementRateLimitRejections();

        // Then
        Counter counter = meterRegistry.find("gateway.ratelimit.rejections.total")
                .counter();

        assertThat(counter).isNotNull();
        assertThat(counter.count()).isEqualTo(2.0);
    }

    @Test
    void shouldRecordAuthServiceLatency() {
        // Given
        Timer.Sample sample = gatewayMetrics.startAuthServiceTimer();

        // Simulate some work
        try {
            Thread.sleep(10);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // When
        gatewayMetrics.stopAuthServiceTimer(sample);

        // Then
        Timer timer = meterRegistry.find("gateway.auth.service.latency")
                .timer();

        assertThat(timer).isNotNull();
        assertThat(timer.count()).isEqualTo(1);
        assertThat(timer.totalTime(java.util.concurrent.TimeUnit.MILLISECONDS)).isGreaterThan(0);
    }

    @Test
    void shouldHaveDescriptiveMetricNames() {
        // When
        gatewayMetrics.recordCacheHit(GatewayMetrics.LAYER_L1, GatewayMetrics.SERVICE_AUTH);
        gatewayMetrics.incrementRateLimitRejections();

        // Then - Verify metric names follow Prometheus conventions
        Counter cacheCounter = meterRegistry.find("gateway.cache.operations.total").counter();
        Counter rateLimitCounter = meterRegistry.find("gateway.ratelimit.rejections.total").counter();
        Timer authTimer = meterRegistry.find("gateway.auth.service.latency").timer();

        assertThat(cacheCounter).isNotNull();
        assertThat(rateLimitCounter).isNotNull();
        assertThat(authTimer).isNotNull();

        // Verify descriptions exist
        assertThat(cacheCounter.getId().getDescription()).isEqualTo("Cache operation results");
        assertThat(rateLimitCounter.getId().getDescription()).isEqualTo("Rate limit rejections");
        assertThat(authTimer.getId().getDescription()).isEqualTo("Auth service call latency");
    }

    @Test
    void shouldUseConstantsForTagValues() {
        // Verify constants are defined and can be used
        assertThat(GatewayMetrics.SERVICE_AUTH).isEqualTo("auth");
        assertThat(GatewayMetrics.SERVICE_ITEMS).isEqualTo("items");
        assertThat(GatewayMetrics.LAYER_L1).isEqualTo("l1");
        assertThat(GatewayMetrics.LAYER_L2).isEqualTo("l2");
        assertThat(GatewayMetrics.RESULT_HIT).isEqualTo("hit");
        assertThat(GatewayMetrics.RESULT_MISS).isEqualTo("miss");
    }
}

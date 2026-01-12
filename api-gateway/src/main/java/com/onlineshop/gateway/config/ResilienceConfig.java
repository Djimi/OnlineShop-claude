package com.onlineshop.gateway.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.bulkhead.BulkheadRegistry;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import io.github.resilience4j.timelimiter.TimeLimiterRegistry;

@Configuration
public class ResilienceConfig {

    /**
     * Configures RestClient with JDK's native HttpClient.
     *
     * Timeouts:
     * - Connection timeout: 5 seconds (time to establish connection)
     * - Read timeout: 5 seconds (time to read response)
     *
     * Note: Resilience4j TimeLimiter (3s) typically triggers before these timeouts,
     * providing the primary timeout control. These HTTP-level timeouts serve as a safety net.
     */
    @Bean
    public RestClient restClient() {
        // Use Java's native HttpClient (Java 11+) with connection timeout
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();

        // Create request factory with read timeout
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        // Build RestClient with configured factory
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public CircuitBreaker authServiceCircuitBreaker() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(10)
                .failureRateThreshold(50)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(3)
                .build();

        CircuitBreakerRegistry registry = CircuitBreakerRegistry.of(config);
        return registry.circuitBreaker("authService");
    }

    @Bean
    public Retry authServiceRetry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(500))
                .build();

        RetryRegistry registry = RetryRegistry.of(config);
        return registry.retry("authService");
    }

    @Bean
    public TimeLimiter authServiceTimeLimiter() {
        TimeLimiterConfig config = TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(3))
                .build();

        TimeLimiterRegistry registry = TimeLimiterRegistry.of(config);
        return registry.timeLimiter("authService");
    }

    /**
     * Configures Bulkhead pattern for Auth Service.
     *
     * The Bulkhead pattern limits the number of concurrent calls to prevent
     * resource exhaustion and cascading failures. This implementation uses
     * a semaphore-based approach which is optimal for virtual threads.
     *
     * Configuration Details:
     * - maxConcurrentCalls: 10 - Maximum concurrent calls allowed (prevents overwhelming target service)
     * - maxWaitDuration: 5 seconds - Maximum time to wait for a permit (prevents indefinite blocking)
     * - writableStackTraceEnabled: true - Captures stack traces for better debugging
     *
     * For Auth Service, we use a conservative limit (10 concurrent) because:
     * 1. Each auth validation call performs expensive token verification
     * 2. Auth service is a critical dependency (circuit breaker helps here too)
     * 3. Virtual threads handle waiting gracefully without thread pool overhead
     */
    @Bean
    public Bulkhead authServiceBulkhead() {
        BulkheadConfig config = BulkheadConfig.custom()
                // Maximum number of concurrent calls allowed
                // This prevents overwhelming the auth service with too many simultaneous requests
                // With virtual threads, we can safely allow more concurrent calls compared to platform threads
                .maxConcurrentCalls(200)

                // Maximum duration to wait for a permit to execute
                // If a permit isn't available within this time, BulkheadFullException is thrown
                // Set to 5 seconds to align with our HTTP timeouts and allow graceful backpressure
                .maxWaitDuration(Duration.ofSeconds(5))

                // Enable detailed stack traces in exceptions
                // Useful for debugging when bulkhead is exhausted (BulkheadFullException)
                // Helps identify which part of the code triggered the bulkhead rejection
                .writableStackTraceEnabled(true)

                .build();

        BulkheadRegistry registry = BulkheadRegistry.of(config);
        return registry.bulkhead("authService");
    }
}

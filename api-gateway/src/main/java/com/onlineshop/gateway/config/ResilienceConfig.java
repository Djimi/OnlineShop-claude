package com.onlineshop.gateway.config;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

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
}

package com.onlineshop.gateway.service.impl;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.decorators.Decorators;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.exception.GatewayTimeoutException;
import com.onlineshop.gateway.exception.ServiceUnavailableException;
import com.onlineshop.gateway.service.AuthServiceClient;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DefaultAuthServiceClient implements AuthServiceClient {

    private final RestClient restClient;
    private final String authServiceUrl;
    private final String validateEndpoint;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    private final Bulkhead bulkhead;

    private final ExecutorService executor;

    public DefaultAuthServiceClient(
            RestClient restClient,
            @Value("${gateway.auth.service-url}") String authServiceUrl,
            @Value("${gateway.auth.validate-endpoint}") String validateEndpoint,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter,
            Bulkhead bulkhead,
            ExecutorService executor) {
        this.restClient = restClient;
        this.authServiceUrl = authServiceUrl;
        this.validateEndpoint = validateEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.timeLimiter = timeLimiter;
        this.bulkhead = bulkhead;
        this.executor = executor;
        };

//    @PostConstruct
    public void setupLogging() {
        // 1. Bulkhead Events
        bulkhead.getEventPublisher()
                .onCallPermitted(event -> log.info("Bulkhead: Call permitted"))
                .onCallRejected(event -> log.warn("Bulkhead: Call rejected!"));

        // 2. TimeLimiter Events
        timeLimiter.getEventPublisher()
                .onSuccess(event -> log.info("TimeLimiter: Finished on time"))
                .onTimeout(event -> log.error("TimeLimiter: Timed out!"));

        // 3. Circuit Breaker Events
        this.circuitBreaker.getEventPublisher()
                .onSuccess(event -> log.info("CircuitBreaker: Success"))
                .onError(event -> log.error("CircuitBreaker: Recorded an error"))
                .onStateTransition(event -> log.info("CircuitBreaker: State changed to {}", event.getStateTransition()));

        // 4. Retry Events
        retry.getEventPublisher()
                .onRetry(event -> log.warn("Retry: Attempt number {}", event.getNumberOfRetryAttempts()))
                .onSuccess(event -> log.info("Retry: Succeeded after retry"))
                .onError(event -> log.error("Retry: Failed after all attempts"));
    }

    @Override
    public Optional<ValidateResponse> validateToken(String token) {
        try {

            // 1. Core Logic
            Supplier<Optional<ValidateResponse>> baseSupplier = () -> callAuthService(token);

            // 2. Bulkhead (Innermost - Semaphore based for Virtual Threads)
            Supplier<Optional<ValidateResponse>> bulkheaded = Bulkhead.decorateSupplier(bulkhead, baseSupplier);

            // 3. TimeLimiter (Executing on Virtual Threads)
            Callable<Optional<ValidateResponse>> timeLimited = TimeLimiter.decorateFutureSupplier(timeLimiter,
                                                                                                  () -> CompletableFuture.supplyAsync(bulkheaded,
                                                                                                                                      executor));

            // 4. Circuit Breaker (Middle)
            Callable<Optional<ValidateResponse>> circuitBreaker = CircuitBreaker.decorateCallable(this.circuitBreaker, timeLimited);

            // 5. Retry (Outermost)
            Callable<Optional<ValidateResponse>> retryable = Retry.decorateCallable(retry, circuitBreaker);

            return retryable.call();
        } catch (TimeoutException e) {
            log.error("Auth service call timed out: {}", e.getMessage());
            throw new GatewayTimeoutException("Auth service request timed out", e);
        } catch (Exception e) {
            log.error("Failed to validate token with Auth service: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service is unavailable", e);
        }
    }

    private Optional<ValidateResponse> callAuthService(String token) {
        try {
//             Use RestClient fluent API
            ValidateResponse response = restClient.get()
                    .uri(authServiceUrl + validateEndpoint)
                    .header("Authorization", "Bearer: " + token)
                    .retrieve()
                    .body(ValidateResponse.class);

            // Auth service now only returns valid responses (no more valid:false)
            // If we get here, the token is valid
            return Optional.ofNullable(response);

        } catch (ResourceAccessException e) {
            log.error("Auth service is unreachable: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service is unreachable", e);
        } catch (RestClientException e) {
            // Auth service throws exceptions for invalid tokens (400, 401, etc.)
            // Missing authorization header → 400 Bad Request
            // Invalid token → 401/403
            log.debug("Token validation failed: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

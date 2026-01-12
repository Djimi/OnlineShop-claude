package com.onlineshop.gateway.service.impl;

import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.function.Supplier;

import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadFullException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.decorators.Decorators;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.exception.GatewayTimeoutException;
import com.onlineshop.gateway.exception.ServiceUnavailableException;
import com.onlineshop.gateway.service.AuthServiceClient;

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

////    @PostConstruct
//    public void setupLogging() {
//        // 1. Bulkhead Events
//        bulkhead.getEventPublisher()
//                .onCallPermitted(event -> log.info("Bulkhead: Call permitted"))
//                .onCallRejected(event -> log.warn("Bulkhead: Call rejected!"));
//
//        // 2. TimeLimiter Events
//        timeLimiter.getEventPublisher()
//                .onSuccess(event -> log.info("TimeLimiter: Finished on time"))
//                .onTimeout(event -> log.error("TimeLimiter: Timed out!"));
//
//        // 3. Circuit Breaker Events
//        this.circuitBreaker.getEventPublisher()
//                .onSuccess(event -> log.info("CircuitBreaker: Success"))
//                .onError(event -> log.error("CircuitBreaker: Recorded an error"))
//                .onStateTransition(event -> log.info("CircuitBreaker: State changed to {}", event.getStateTransition()));
//
//        // 4. Retry Events
//        retry.getEventPublisher()
//                .onRetry(event -> log.warn("Retry: Attempt number {}", event.getNumberOfRetryAttempts()))
//                .onSuccess(event -> log.info("Retry: Succeeded after retry"))
//                .onError(event -> log.error("Retry: Failed after all attempts"));
//    }

    @Override
    public Optional<ValidateResponse> validateToken(String token) {
        try {
            // 1. Core Logic
            Supplier<ValidateResponse> baseSupplier = () -> callAuthService(token);

            // 2. Bulkhead (Innermost - Semaphore based for Virtual Threads)
            Supplier<ValidateResponse> bulkheaded = Bulkhead.decorateSupplier(bulkhead, baseSupplier);

            // 3. TimeLimiter (Executing on Virtual Threads)
            Callable<ValidateResponse> timeLimited = TimeLimiter.decorateFutureSupplier(timeLimiter,
                                                                                        () -> CompletableFuture.supplyAsync(bulkheaded,
                                                                                                                            executor));

            // 4. Circuit Breaker (Middle)
            Callable<ValidateResponse> circuitBreaker = CircuitBreaker.decorateCallable(this.circuitBreaker, timeLimited);

            // 5. Retry (Outermost)
            Callable<ValidateResponse> retryable = Retry.decorateCallable(retry, circuitBreaker);

            return Optional.ofNullable(retryable.call());
        } catch (CallNotPermittedException e) {
            // Circuit breaker is open - auth service has too many failures
            log.warn("Circuit breaker is open for auth service: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service circuit breaker is open", e);
        } catch (BulkheadFullException e) {
            // Bulkhead is exhausted - too many concurrent requests
            log.warn("Bulkhead is exhausted for auth service: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service is overloaded", e);
        } catch (TimeoutException e) {
            // TimeLimiter timeout - request took too long
            log.error("Auth service call timed out: {}", e.getMessage());
            throw new GatewayTimeoutException("Auth service request timed out", e);
        } catch (HttpMessageNotReadableException e) {
            // Parsing error - auth service returned invalid JSON/response format
            log.warn("Auth service returned unparseable response: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service returned invalid response format", e);
        } catch (Exception e) {
            // Any other exceptions from REST client or resilience4j
            log.error("Failed to validate token with Auth service: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service is unavailable", e);
        }
    }

    private ValidateResponse callAuthService(String token) {
            // Use RestClient fluent API
        ValidateResponse response = restClient.get()
                .uri(authServiceUrl + validateEndpoint)
                .header("Authorization", "Bearer: " + token)
                .retrieve()
                .body(ValidateResponse.class);

        // Auth service now only returns valid responses (no more valid:false)
        // If we get here, the token is valid
        return response;

    }
}

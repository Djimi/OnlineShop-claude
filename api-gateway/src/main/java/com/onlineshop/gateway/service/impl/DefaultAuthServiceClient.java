package com.onlineshop.gateway.service.impl;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

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

    public DefaultAuthServiceClient(
            RestClient restClient,
            @Value("${gateway.auth.service-url}") String authServiceUrl,
            @Value("${gateway.auth.validate-endpoint}") String validateEndpoint,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter) {
        this.restClient = restClient;
        this.authServiceUrl = authServiceUrl;
        this.validateEndpoint = validateEndpoint;
        this.circuitBreaker = circuitBreaker;
        this.retry = retry;
        this.timeLimiter = timeLimiter;
    }

    @Override
    public Optional<ValidateResponse> validateToken(String token) {
        try {
            Callable<Optional<ValidateResponse>> callable = () -> callAuthService(token);

            // Decorate with retry, circuit breaker, and time limiter
            callable = Retry.decorateCallable(retry, callable);
            callable = CircuitBreaker.decorateCallable(circuitBreaker, callable);
            callable = TimeLimiter.decorateFutureSupplier(timeLimiter,
                    () -> CompletableFuture.supplyAsync(() -> {
                        try {
                            return callAuthService(token);
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }));

            return callable.call();

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
            // Use RestClient fluent API
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

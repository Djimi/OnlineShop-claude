package com.onlineshop.gateway.service.impl;

import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.exception.GatewayTimeoutException;
import com.onlineshop.gateway.exception.ServiceUnavailableException;
import com.onlineshop.gateway.service.AuthServiceClient;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;

@Service
@Slf4j
public class DefaultAuthServiceClient implements AuthServiceClient {

    private final RestTemplate restTemplate;
    private final String authServiceUrl;
    private final String validateEndpoint;
    private final CircuitBreaker circuitBreaker;
    private final Retry retry;
    private final TimeLimiter timeLimiter;

    public DefaultAuthServiceClient(
            RestTemplate restTemplate,
            @Value("${gateway.auth.service-url}") String authServiceUrl,
            @Value("${gateway.auth.validate-endpoint}") String validateEndpoint,
            CircuitBreaker circuitBreaker,
            Retry retry,
            TimeLimiter timeLimiter) {
        this.restTemplate = restTemplate;
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
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer: " + token);

            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ValidateResponse> response = restTemplate.exchange(
                    authServiceUrl + validateEndpoint,
                    HttpMethod.GET,
                    entity,
                    ValidateResponse.class
            );

            ValidateResponse validateResponse = response.getBody();
            if (validateResponse != null && validateResponse.isValid()) {
                return Optional.of(validateResponse);
            }

            return Optional.empty();

        } catch (ResourceAccessException e) {
            log.error("Auth service is unreachable: {}", e.getMessage());
            throw new ServiceUnavailableException("Auth service is unreachable", e);
        } catch (RestClientException e) {
            log.error("Failed to validate token with Auth service: {}", e.getMessage());
            return Optional.empty();
        }
    }
}

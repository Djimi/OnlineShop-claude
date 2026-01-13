package com.onlineshop.gateway.service.impl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.bulkhead.annotation.Bulkhead;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.service.AuthServiceClient;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class DefaultAuthServiceClient implements AuthServiceClient {

    private final RestClient restClient;
    private final String authServiceUrl;
    private final String validateEndpoint;
    private final ExecutorService virtualThreadExecutor;

    public DefaultAuthServiceClient(
            RestClient restClient,
            @Value("${gateway.auth.service-url}") String authServiceUrl,
            @Value("${gateway.auth.validate-endpoint}") String validateEndpoint,
            ExecutorService virtualThreadExecutor) {
        this.restClient = restClient;
        this.authServiceUrl = authServiceUrl;
        this.validateEndpoint = validateEndpoint;
        this.virtualThreadExecutor = virtualThreadExecutor;
    }

    @Override
    @TimeLimiter(name = "authService")
    @CircuitBreaker(name = "authService")
    @Bulkhead(name = "authService")
    public CompletableFuture<ValidateResponse> validateToken(String token) {
        return CompletableFuture.supplyAsync(
            () -> callAuthService(token),
            virtualThreadExecutor
        );
    }

    private ValidateResponse callAuthService(String token) {
        // Use RestClient fluent API
        ValidateResponse response = restClient.get()
                .uri(authServiceUrl + validateEndpoint)
                .header("Authorization", "Bearer: " + token)
                .retrieve()
                .body(ValidateResponse.class);

        return response;
    }
}

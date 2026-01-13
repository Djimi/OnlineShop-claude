package com.onlineshop.gateway.service;

import com.onlineshop.gateway.dto.ValidateResponse;
import java.util.concurrent.CompletableFuture;

/**
 * Interface for communication with the Auth Service.
 * Provides token validation through HTTP calls.
 */
public interface AuthServiceClient {

    /**
     * Validates a token by calling the Auth Service.
     *
     * @param token the authentication token to validate
     * @return CompletableFuture containing ValidateResponse if token is valid, or exception if invalid/error
     */
    CompletableFuture<ValidateResponse> validateToken(String token);
}

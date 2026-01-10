package com.onlineshop.gateway.service;

import com.onlineshop.gateway.dto.ValidateResponse;

import java.util.Optional;

/**
 * Interface for communication with the Auth Service.
 * Provides token validation through HTTP calls.
 */
public interface AuthServiceClient {

    /**
     * Validates a token by calling the Auth Service.
     *
     * @param token the authentication token to validate
     * @return ValidateResponse if validation succeeds, empty otherwise
     */
    Optional<ValidateResponse> validateToken(String token);
}

package com.onlineshop.gateway.service;

import com.onlineshop.gateway.dto.ValidateResponse;

/**
 * Interface for communication with the Auth Service.
 * Provides token validation through HTTP calls.
 */
public interface AuthServiceClient {

    /**
     * Validates a token by calling the Auth Service.
     *
     * @param token the authentication token to validate
     * @return ValidateResponse if token is valid, null if invalid or error
     */
    ValidateResponse validateToken(String token);
}

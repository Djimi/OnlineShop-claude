package com.onlineshop.gateway.exception;

/**
 * Base exception for token validation failures.
 */
public class TokenValidationException extends RuntimeException {

    public TokenValidationException(String message) {
        super(message);
    }

    public TokenValidationException(String message, Throwable cause) {
        super(message, cause);
    }
}

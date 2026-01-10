package com.onlineshop.gateway.exception;

/**
 * Exception thrown when rate limit is exceeded.
 */
public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException(String message) {
        super(message);
    }
}

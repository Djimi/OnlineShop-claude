package com.onlineshop.gateway.exception;

/**
 * Exception thrown when a gateway operation times out.
 */
public class GatewayTimeoutException extends RuntimeException {

    public GatewayTimeoutException(String message) {
        super(message);
    }

    public GatewayTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}

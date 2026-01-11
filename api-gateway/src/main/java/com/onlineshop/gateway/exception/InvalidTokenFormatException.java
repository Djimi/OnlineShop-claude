package com.onlineshop.gateway.exception;

/**
 * Exception thrown when a token fails format validation or sanitization checks.
 * This indicates a security concern - the token doesn't meet basic security requirements
 * (null, too long, contains null bytes, etc).
 */
public class InvalidTokenFormatException extends TokenValidationException {

    public InvalidTokenFormatException(String message) {
        super(message);
    }

    public InvalidTokenFormatException(String message, Throwable cause) {
        super(message, cause);
    }
}

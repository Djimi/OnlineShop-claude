package com.onlineshop.auth.exception;

public class InvalidTokenFormatException extends RuntimeException {

    public InvalidTokenFormatException() {
        super("Invalid token format");
    }

    public InvalidTokenFormatException(String message) {
        super(message);
    }
}

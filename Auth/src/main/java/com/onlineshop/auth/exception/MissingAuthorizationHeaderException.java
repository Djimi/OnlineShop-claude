package com.onlineshop.auth.exception;

public class MissingAuthorizationHeaderException extends RuntimeException {

    public MissingAuthorizationHeaderException() {
        super("Authorization header is required");
    }

    public MissingAuthorizationHeaderException(String message) {
        super(message);
    }
}

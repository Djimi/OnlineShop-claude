package com.onlineshop.gateway.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TokenSanitizer {

    private static final int MAX_TOKEN_LENGTH = 8192; // 8KB

    /**
     * Validates and sanitizes a token.
     *
     * @param token the token to validate
     * @return true if token is valid, false otherwise
     */
    public boolean isValid(String token) {
        if (token == null || token.isBlank()) {
            log.debug("Token validation failed: token is null or empty");
            return false;
        }

        if (token.length() > MAX_TOKEN_LENGTH) {
            log.warn("Token validation failed: token exceeds maximum length of {} bytes", MAX_TOKEN_LENGTH);
            return false;
        }

        if (containsNullByte(token)) {
            log.warn("Token validation failed: token contains null bytes");
            return false;
        }

        return true;
    }

    /**
     * Sanitizes username before adding to headers.
     *
     * @param username the username to sanitize
     * @return sanitized username
     */
    public String sanitizeUsername(String username) {
        if (username == null) {
            return "";
        }

        // Remove CRLF characters to prevent header injection
        return username.replaceAll("[\\r\\n]", "");
    }

    private boolean containsNullByte(String token) {
        return token.indexOf('\0') != -1;
    }
}

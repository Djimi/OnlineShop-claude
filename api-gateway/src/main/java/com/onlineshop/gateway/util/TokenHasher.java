package com.onlineshop.gateway.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing tokens using SHA-256.
 * Used for secure cache key generation to avoid storing raw tokens.
 */
public final class TokenHasher {

    private TokenHasher() {
        // Utility class - prevent instantiation
    }

    /**
     * Hashes a token using SHA-256.
     * This method is designed to be called from SpEL expressions in @Cacheable annotations.
     *
     * @param token the raw token to hash
     * @return the hex-encoded SHA-256 hash of the token
     * @throws IllegalStateException if SHA-256 algorithm is not available
     */
    public static String hash(String token) {
        if (token == null) {
            return null;
        }
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}

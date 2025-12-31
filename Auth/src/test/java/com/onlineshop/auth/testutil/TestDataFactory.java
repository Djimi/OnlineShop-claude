package com.onlineshop.auth.testutil;

import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

public final class TestDataFactory {

    private TestDataFactory() {
    }

    public static User createUser(String username, String passwordHash) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        return user;
    }

    public static Session createSession(String tokenHash, User user, Instant expiresAt) {
        Session session = new Session();
        session.setTokenHash(tokenHash);
        session.setUser(user);
        session.setExpiresAt(expiresAt);
        return session;
    }

    public static RegisterRequest createRegisterRequest(String username, String password) {
        return new RegisterRequest(username, password);
    }

    public static LoginRequest createLoginRequest(String username, String password) {
        return new LoginRequest(username, password);
    }

    public static String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not available", e);
        }
    }
}

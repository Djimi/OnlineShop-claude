package com.onlineshop.auth.service;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.exception.InvalidCredentialsException;
import com.onlineshop.auth.exception.InvalidTokenException;
import com.onlineshop.auth.exception.UserAlreadyExistsException;
import com.onlineshop.auth.repository.SessionRepository;
import com.onlineshop.auth.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final long sessionExpirationSeconds;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder,
            @Value("${session.expiration:3600}") long sessionExpirationSeconds) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionExpirationSeconds = sessionExpirationSeconds;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);
        // entityManager.refresh(user);

        return RegisterResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant expiresAt = Instant.now().plusSeconds(sessionExpirationSeconds);

        Session session = new Session();
        session.setTokenHash(tokenHash);
        session.setUser(user);
        session.setExpiresAt(expiresAt);

        sessionRepository.save(session);

        return LoginResponse.builder()
                .token(token)
                .userId(user.getId())
                .username(user.getUsername())
                .createdAt(session.getCreatedAt())
                .expiresAt(expiresAt)
                .tokenType("Bearer")
                .expiresIn(sessionExpirationSeconds)
                .build();
    }

    @Transactional(readOnly = true)
    public ValidateResponse validateToken(String token) {
        String tokenHash = hashToken(token);
        Session session = sessionRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidTokenException::new);

        if (session.isExpired()) {
            throw new InvalidTokenException();
        }

        User user = session.getUser();
        return ValidateResponse.builder()
                .valid(true)
                .userId(user.getId())
                .username(user.getUsername())
                .createdAt(session.getCreatedAt())
                .expiresAt(session.getExpiresAt())
                .build();
    }

    private String generateToken() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes());
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not available", e);
        }
    }
}

package com.onlineshop.auth.service;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.exception.InvalidUsernameOrPasswordException;
import com.onlineshop.auth.exception.UserAlreadyExistsException;
import com.onlineshop.auth.repository.SessionRepository;
import com.onlineshop.auth.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom;
    private final Clock clock;
    private final long sessionExpirationSeconds;

    public AuthService(UserRepository userRepository, SessionRepository sessionRepository,
            PasswordEncoder passwordEncoder, SecureRandom secureRandom, Clock clock,
            @Value("${session.expiration:3600}") long sessionExpirationSeconds) {
        this.userRepository = userRepository;
        this.sessionRepository = sessionRepository;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = secureRandom;
        this.clock = clock;
        this.sessionExpirationSeconds = sessionExpirationSeconds;
    }

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase();
        if (userRepository.existsByNormalizedUsername(normalizedUsername)) {
            throw new UserAlreadyExistsException(request.getUsername());
        }

        User user = new User(request.getUsername(), passwordEncoder.encode(request.getPassword()));
        User savedUser = userRepository.save(user);

        return RegisterResponse.builder()
                .userId(savedUser.getId())
                .username(savedUser.getUsername())
                .createdAt(savedUser.getCreatedAt())
                .updatedAt(savedUser.getUpdatedAt())
                .build();
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String normalizedUsername = request.getUsername().toLowerCase();
        User user = userRepository.findByNormalizedUsername(normalizedUsername)
                .orElseThrow(InvalidUsernameOrPasswordException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidUsernameOrPasswordException();
        }

        String token = generateToken();
        String tokenHash = hashToken(token);
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(sessionExpirationSeconds);

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
                .orElse(null);

        if (session == null || !session.isValid(clock.instant())) {
            return ValidateResponse.builder()
                    .valid(false)
                    .build();
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
        secureRandom.nextBytes(bytes);
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

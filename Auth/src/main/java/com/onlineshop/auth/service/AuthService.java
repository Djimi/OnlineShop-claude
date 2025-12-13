package com.onlineshop.auth.service;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.exception.InvalidCredentialsException;
import com.onlineshop.auth.exception.InvalidTokenException;
import com.onlineshop.auth.exception.UserAlreadyExistsException;
import com.onlineshop.auth.repository.SessionRepository;
import com.onlineshop.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final SessionRepository sessionRepository;
    private final PasswordEncoder passwordEncoder;

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${session.expiration:3600}")
    private long sessionExpirationSeconds;

    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UserAlreadyExistsException(request.getUsername());
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        userRepository.save(user);

        return new RegisterResponse("User registered successfully");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        String token = generateToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(sessionExpirationSeconds);

        Session session = new Session();
        session.setToken(token);
        session.setUser(user);
        session.setExpiresAt(expiresAt);

        sessionRepository.save(session);

        return LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(sessionExpirationSeconds)
                .build();
    }

    @Transactional(readOnly = true)
    public ValidateResponse validateToken(String token) {
        Session session = sessionRepository.findByToken(token)
                .orElseThrow(InvalidTokenException::new);

        if (session.isExpired()) {
            throw new InvalidTokenException();
        }

        User user = session.getUser();

        return ValidateResponse.builder()
                .valid(true)
                .userId(user.getId())
                .username(user.getUsername())
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
}

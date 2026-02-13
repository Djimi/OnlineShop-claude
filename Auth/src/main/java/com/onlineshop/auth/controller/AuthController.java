package com.onlineshop.auth.controller;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.exception.MissingAuthorizationHeaderException;
import com.onlineshop.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        long startedAt = System.nanoTime();
        log.info("Register request started");
        RegisterResponse response = authService.register(request);
        log.info("Register request completed in {} ms", elapsedMillis(startedAt));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        long startedAt = System.nanoTime();
        log.info("Login request started");
        LoginResponse response = authService.login(request);
        log.info("Login request completed in {} ms", elapsedMillis(startedAt));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        long startedAt = System.nanoTime();
        log.info("Validate request started");
        if (authHeader == null) {
            throw new MissingAuthorizationHeaderException();
        }
        String token = extractToken(authHeader);
        if (token == null) {
            log.info("Validate request completed with valid=false in {} ms", elapsedMillis(startedAt));
            return ResponseEntity.ok(ValidateResponse.builder().valid(false).build());
        }
        ValidateResponse response = authService.validateToken(token);
        log.info("Validate request completed with valid={} in {} ms",
                response.isValid(), elapsedMillis(startedAt));
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (authHeader.isBlank()) {
            return null;
        }
        if (!authHeader.startsWith(BEARER_PREFIX)) {
            return null;
        }
        String token = authHeader.substring(BEARER_PREFIX.length());
        if (token.isBlank()) {
            return null;
        }
        return token;
    }

    private long elapsedMillis(long startedAt) {
        return (System.nanoTime() - startedAt) / 1_000_000;
    }
}

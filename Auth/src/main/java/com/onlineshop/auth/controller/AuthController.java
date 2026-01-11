package com.onlineshop.auth.controller;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.exception.InvalidTokenFormatException;
import com.onlineshop.auth.exception.MissingAuthorizationHeaderException;
import com.onlineshop.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/validate")
    public ResponseEntity<ValidateResponse> validate(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null) {
            throw new MissingAuthorizationHeaderException();
        }
        String token = extractToken(authHeader);
        ValidateResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (authHeader.isBlank()) {
            throw new InvalidTokenFormatException("Authorization header cannot be empty");
        }
        if (!authHeader.startsWith("Bearer: ")) {
            throw new InvalidTokenFormatException("Authorization header must start with 'Bearer: '");
        }
        String token = authHeader.substring(8);
        if (token.isBlank()) {
            throw new InvalidTokenFormatException("Token cannot be empty");
        }
        return token;
    }
}

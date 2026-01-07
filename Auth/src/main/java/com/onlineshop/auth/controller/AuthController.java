package com.onlineshop.auth.controller;

import com.onlineshop.auth.dto.*;
import com.onlineshop.auth.exception.InvalidTokenException;
import com.onlineshop.auth.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
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
            @RequestHeader("Authorization")
            @NotBlank(message = "Authorization header is required")
            String authHeader) {
        String token = extractToken(authHeader);
        ValidateResponse response = authService.validateToken(token);
        return ResponseEntity.ok(response);
    }

    private String extractToken(String authHeader) {
        if (!authHeader.startsWith("Bearer: ")) {
            throw new InvalidTokenException();
        }
        return authHeader.substring(8);
    }
}

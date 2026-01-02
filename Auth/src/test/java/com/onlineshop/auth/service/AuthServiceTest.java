package com.onlineshop.auth.service;

import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.LoginResponse;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.dto.RegisterResponse;
import com.onlineshop.auth.dto.ValidateResponse;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.exception.InvalidTokenException;
import com.onlineshop.auth.exception.InvalidUsernameOrPasswordException;
import com.onlineshop.auth.exception.UserAlreadyExistsException;
import com.onlineshop.auth.repository.SessionRepository;
import com.onlineshop.auth.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final long SESSION_EXPIRATION_SECONDS = 3600L;
    private static final Instant FIXED_TIME = Instant.parse("2025-01-15T10:00:00Z");

    @Mock
    private UserRepository userRepository;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private SecureRandom secureRandom;

    private Clock fixedClock;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        fixedClock = Clock.fixed(FIXED_TIME, ZoneId.of("UTC"));
        authService = new AuthService(
                userRepository,
                sessionRepository,
                passwordEncoder,
                secureRandom,
                fixedClock,
                SESSION_EXPIRATION_SECONDS
        );
    }

    // ==================== register() tests ====================

    @Test
    void register_whenUsernameAvailable_createsUserAndReturnsResponse() {
        RegisterRequest request = new RegisterRequest("testuser", "password123");

        when(userRepository.existsByNormalizedUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(argThat(user ->
                "testuser".equals(user.getUsername()) &&
                "encodedPassword".equals(user.getPasswordHash())
        ))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            user.setCreatedAt(FIXED_TIME);
            user.setUpdatedAt(FIXED_TIME);
            return user;
        });

        RegisterResponse response = authService.register(request);

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getCreatedAt()).isEqualTo(FIXED_TIME);
        assertThat(response.getUpdatedAt()).isEqualTo(FIXED_TIME);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();
        assertThat(savedUser.getUsername()).isEqualTo("testuser");
        assertThat(savedUser.getPasswordHash()).isEqualTo("encodedPassword");
    }

    @Test
    void register_whenUsernameExists_throwsUserAlreadyExistsException() {
        RegisterRequest request = new RegisterRequest("existinguser", "password123");
        when(userRepository.existsByNormalizedUsername("existinguser")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_whenUsernameExistsWithDifferentCase_throwsUserAlreadyExistsException() {
        // Scenario: "ivan" exists in DB, user tries to register "Ivan" -> should be rejected
        RegisterRequest request = new RegisterRequest("Ivan", "password123");

        // The service normalizes to lowercase before checking, so "Ivan" -> "ivan"
        when(userRepository.existsByNormalizedUsername("ivan")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(UserAlreadyExistsException.class);
    }

    @Test
    void register_savesUserWithEncodedPassword() {
        RegisterRequest request = new RegisterRequest("testuser", "rawPassword");
        when(userRepository.existsByNormalizedUsername("testuser")).thenReturn(false);
        when(passwordEncoder.encode("rawPassword")).thenReturn("hashedPassword123");
        when(userRepository.save(argThat(user ->
                "testuser".equals(user.getUsername()) &&
                "hashedPassword123".equals(user.getPasswordHash())
        ))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getPasswordHash()).isEqualTo("hashedPassword123");
    }

    // ==================== login() tests ====================

    @Test
    void login_whenCredentialsValid_returnsTokenAndSession() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = createUser(1L, "testuser", "encodedPassword");

        when(userRepository.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        mockSecureRandomBytes();
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(1L);
            session.setCreatedAt(FIXED_TIME);
            return session;
        });

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).isNotNull();
        assertThat(response.getToken()).hasSize(64); // 32 bytes = 64 hex chars
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(SESSION_EXPIRATION_SECONDS);
        verify(sessionRepository).save(any(Session.class));
    }

    @Test
    void login_whenUserNotFound_throwsInvalidUsernameOrPasswordException() {
        LoginRequest request = new LoginRequest("nonexistent", "password123");
        when(userRepository.findByNormalizedUsername("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidUsernameOrPasswordException.class);
    }

    @Test
    void login_whenPasswordInvalid_throwsInvalidUsernameOrPasswordException() {
        LoginRequest request = new LoginRequest("testuser", "wrongPassword");
        User user = createUser(1L, "testuser", "encodedPassword");

        when(userRepository.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(InvalidUsernameOrPasswordException.class);
    }

    @Test
    void login_generatesUniqueTokensForEachCall() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = createUser(1L, "testuser", "encodedPassword");

        when(userRepository.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(1L);
            session.setCreatedAt(FIXED_TIME);
            return session;
        });

        // First call returns bytes that produce token1
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) i;
            }
            return null;
        }).doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) (i + 100);
            }
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));

        LoginResponse response1 = authService.login(request);
        LoginResponse response2 = authService.login(request);

        assertThat(response1.getToken()).isNotEqualTo(response2.getToken());
    }

    @Test
    void login_setsCorrectSessionExpiration() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = createUser(1L, "testuser", "encodedPassword");

        when(userRepository.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        mockSecureRandomBytes();
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(1L);
            session.setCreatedAt(FIXED_TIME);
            return session;
        });

        LoginResponse response = authService.login(request);

        Instant expectedExpiration = FIXED_TIME.plusSeconds(SESSION_EXPIRATION_SECONDS);
        assertThat(response.getExpiresAt()).isEqualTo(expectedExpiration);
    }

    // ==================== validateToken() tests ====================

    @Test
    void validateToken_whenTokenValidAndNotExpired_returnsValidateResponse() {
        String token = "abc123token";
        String tokenHash = hashToken(token);
        User user = createUser(1L, "testuser", "encodedPassword");
        Session session = createSession(1L, tokenHash, user, FIXED_TIME.plusSeconds(3600));

        when(sessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(session));

        ValidateResponse response = authService.validateToken(token);

        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getUsername()).isEqualTo("testuser");
    }

    @Test
    void validateToken_whenTokenNotFound_throwsInvalidTokenException() {
        String token = "nonexistenttoken";
        String tokenHash = hashToken(token);

        when(sessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_whenSessionExpired_throwsInvalidTokenException() {
        String token = "expiredtoken";
        String tokenHash = hashToken(token);
        User user = createUser(1L, "testuser", "encodedPassword");
        // Session expired 1 second before FIXED_TIME
        Session session = createSession(1L, tokenHash, user, FIXED_TIME.minusSeconds(1));

        when(sessionRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> authService.validateToken(token))
                .isInstanceOf(InvalidTokenException.class);
    }

    @Test
    void validateToken_hashesTokenBeforeLookup() {
        String token = "testtoken123";
        String expectedHash = hashToken(token);
        User user = createUser(1L, "testuser", "encodedPassword");
        Session session = createSession(1L, expectedHash, user, FIXED_TIME.plusSeconds(3600));

        when(sessionRepository.findByTokenHash(expectedHash)).thenReturn(Optional.of(session));

        authService.validateToken(token);

        ArgumentCaptor<String> hashCaptor = ArgumentCaptor.forClass(String.class);
        verify(sessionRepository).findByTokenHash(hashCaptor.capture());
        assertThat(hashCaptor.getValue()).isEqualTo(expectedHash);
    }

    // ==================== Token generation tests ====================

    @Test
    void login_generatedTokenHasCorrectFormat() {
        LoginRequest request = new LoginRequest("testuser", "password123");
        User user = createUser(1L, "testuser", "encodedPassword");

        when(userRepository.findByNormalizedUsername("testuser")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "encodedPassword")).thenReturn(true);
        mockSecureRandomBytes();
        when(sessionRepository.save(any(Session.class))).thenAnswer(invocation -> {
            Session session = invocation.getArgument(0);
            session.setId(1L);
            session.setCreatedAt(FIXED_TIME);
            return session;
        });

        LoginResponse response = authService.login(request);

        assertThat(response.getToken()).hasSize(64);
        assertThat(response.getToken()).matches("^[0-9a-f]+$");
    }

    @Test
    void login_tokenHashIsDeterministic() {
        String token = "deterministic-token-test";
        String hash1 = hashToken(token);
        String hash2 = hashToken(token);

        assertThat(hash1).isEqualTo(hash2);
        assertThat(hash1).hasSize(64); // SHA-256 produces 32 bytes = 64 hex chars
    }

    // ==================== Helper methods ====================

    private User createUser(Long id, String username, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setPasswordHash(passwordHash);
        user.setCreatedAt(FIXED_TIME);
        user.setUpdatedAt(FIXED_TIME);
        return user;
    }

    private Session createSession(Long id, String tokenHash, User user, Instant expiresAt) {
        Session session = new Session();
        session.setId(id);
        session.setTokenHash(tokenHash);
        session.setUser(user);
        session.setCreatedAt(FIXED_TIME);
        session.setExpiresAt(expiresAt);
        return session;
    }

    private void mockSecureRandomBytes() {
        doAnswer(invocation -> {
            byte[] bytes = invocation.getArgument(0);
            for (int i = 0; i < bytes.length; i++) {
                bytes[i] = (byte) i;
            }
            return null;
        }).when(secureRandom).nextBytes(any(byte[].class));
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

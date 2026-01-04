package com.onlineshop.auth.component.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.LoginResponse;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.dto.RegisterResponse;
import com.onlineshop.auth.dto.ValidateResponse;
import com.onlineshop.auth.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureRestTestClient
class ComponentIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ComponentIT.class);

    private static final String USERNAME = "TestUser";
    private static final String USERNAME_NORMALIZED = "testuser";
    private static final String PASSWORD = "password123";
    private static final Long EXPECTED_USER_ID = 1L;
    private static final int SESSION_EXPIRATION_SECONDS = 3600;

    @Autowired
    private RestTestClient restTestClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    @Test
    void fullAuthenticationFlow_registerLoginValidateMultipleSessions_succeeds() throws Exception {
        // Register user
        RegisterResponse registerResponse = registerUser(USERNAME, PASSWORD);
        assertRegisterResponse(registerResponse, EXPECTED_USER_ID, USERNAME);

        // First login
        LoginResponse firstLogin = login(USERNAME, PASSWORD);
        assertLoginResponse(firstLogin, EXPECTED_USER_ID, USERNAME);

        // Validate first token
        ValidateResponse firstValidation = validateToken(firstLogin.getToken());
        assertValidateResponse(firstValidation, EXPECTED_USER_ID, USERNAME, firstLogin);

        // Second login (creating another session)
        LoginResponse secondLogin = login(USERNAME, PASSWORD);
        assertLoginResponse(secondLogin, EXPECTED_USER_ID, USERNAME);
        assertThat(secondLogin.getToken()).isNotEqualTo(firstLogin.getToken());

        // Both tokens should still be valid (multiple sessions supported)
        assertTokenIsValid(firstLogin.getToken());
        assertTokenIsValid(secondLogin.getToken());

        // Verify database state
        verifyUserInDatabase(EXPECTED_USER_ID, USERNAME, USERNAME_NORMALIZED, PASSWORD);
        verifySessionsInDatabase(firstLogin.getToken(), secondLogin.getToken());

        log.info("Full authentication flow completed successfully");
    }

    // ==================== HTTP Request Helpers ====================

    private RegisterResponse registerUser(String username, String password) throws Exception {
        log.info("Registering user: {}", username);
        return postRequest(
                "/api/v1/auth/register",
                new RegisterRequest(username, password),
                RegisterResponse.class,
                HttpStatus.CREATED
        );
    }

    private LoginResponse login(String username, String password) throws Exception {
        log.info("Logging in user: {}", username);
        return postRequest(
                "/api/v1/auth/login",
                new LoginRequest(username, password),
                LoginResponse.class,
                HttpStatus.OK
        );
    }

    private ValidateResponse validateToken(String token) throws Exception {
        log.info("Validating token");
        var result = restTestClient.get()
                .uri("/api/v1/auth/validate")
                .header("Authorization", "Bearer: " + token)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        return objectMapper.readValue(result.getResponseBody(), ValidateResponse.class);
    }

    private <T> T postRequest(String uri, Object body, Class<T> responseType, HttpStatus expectedStatus) throws Exception {
        var result = restTestClient.post()
                .uri(uri)
                .body(body)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .returnResult(String.class);

        return objectMapper.readValue(result.getResponseBody(), responseType);
    }

    // ==================== Assertion Helpers ====================

    private void assertRegisterResponse(RegisterResponse response, Long expectedUserId, String expectedUsername) {
        Instant now = Instant.now();
        assertThat(response.getUserId()).isEqualTo(expectedUserId);
        assertThat(response.getUsername()).isEqualTo(expectedUsername);
        assertTimestampIsRecent(response.getCreatedAt(), now);
        assertTimestampIsRecent(response.getUpdatedAt(), now);
    }

    private void assertLoginResponse(LoginResponse response, Long expectedUserId, String expectedUsername) {
        Instant now = Instant.now();
        assertThat(response.getToken()).isNotBlank();
        assertThat(response.getUserId()).isEqualTo(expectedUserId);
        assertThat(response.getUsername()).isEqualTo(expectedUsername);
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(SESSION_EXPIRATION_SECONDS);
        assertTimestampIsRecent(response.getCreatedAt(), now);
        assertTimestampIsRecent(response.getExpiresAt(), now.plusSeconds(SESSION_EXPIRATION_SECONDS));
    }

    private void assertValidateResponse(ValidateResponse response, Long expectedUserId, String expectedUsername,
                                        LoginResponse loginResponse) {
        assertThat(response.isValid()).isTrue();
        assertThat(response.getUserId()).isEqualTo(expectedUserId);
        assertThat(response.getUsername()).isEqualTo(expectedUsername);
        assertThat(response.getCreatedAt()).isEqualTo(loginResponse.getCreatedAt());
        assertThat(response.getExpiresAt()).isEqualTo(loginResponse.getExpiresAt());
    }

    private void assertTokenIsValid(String token) throws Exception {
        ValidateResponse response = validateToken(token);
        assertThat(response.isValid()).isTrue();
    }

    private void assertTimestampIsRecent(Instant actual, Instant expected) {
        assertThat(actual).isNotNull();
        long diffSeconds = Math.abs(Duration.between(actual, expected).toSeconds());
        assertThat(diffSeconds)
                .as("Timestamp %s should be within 1 second of %s", actual, expected)
                .isLessThanOrEqualTo(1);
    }

    // ==================== Database Verification Helpers ====================

    private void verifyUserInDatabase(Long expectedId, String expectedUsername,
                                      String expectedNormalizedUsername, String password) {
        log.info("Verifying users table");
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");
        assertThat(users).hasSize(1);

        Map<String, Object> user = users.getFirst();
        assertUserColumns(user);
        assertThat(user.get("id")).isEqualTo(expectedId);
        assertThat(user.get("username")).isEqualTo(expectedUsername);
        assertThat(user.get("normalized_username")).isEqualTo(expectedNormalizedUsername);

        String storedPasswordHash = (String) user.get("password_hash");
        assertThat(storedPasswordHash).isNotBlank();
        assertThat(passwordEncoder.matches(password, storedPasswordHash)).isTrue();

        assertThat((Timestamp) user.get("created_at")).isNotNull();
        assertThat((Timestamp) user.get("updated_at")).isNotNull();
    }

    private void assertUserColumns(Map<String, Object> user) {
        assertThat(user).containsKey("id");
        assertThat(user).containsKey("username");
        assertThat(user).containsKey("normalized_username");
        assertThat(user).containsKey("password_hash");
        assertThat(user).containsKey("created_at");
        assertThat(user).containsKey("updated_at");
        assertThat(user).hasSize(6);
    }

    private void verifySessionsInDatabase(String token1, String token2) {
        log.info("Verifying sessions table");
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList("SELECT * FROM sessions ORDER BY id");
        assertThat(sessions).hasSize(2);

        verifySession(sessions.get(0), token1);
        verifySession(sessions.get(1), token2);

        assertThat(sessions.get(0).get("token_hash"))
                .isNotEqualTo(sessions.get(1).get("token_hash"));

        log.info("Database verification completed: 1 user, 2 sessions");
    }

    private void verifySession(Map<String, Object> session, String token) {
        assertSessionColumns(session);

        String expectedTokenHash = TestDataFactory.hashToken(token);
        assertThat(session.get("token_hash")).isEqualTo(expectedTokenHash);
        assertThat(session.get("user_id")).isEqualTo(EXPECTED_USER_ID);

        Timestamp createdAt = (Timestamp) session.get("created_at");
        Timestamp expiresAt = (Timestamp) session.get("expires_at");
        assertThat(createdAt).isNotNull();
        assertThat(expiresAt).isNotNull();

        long durationSeconds = (expiresAt.getTime() - createdAt.getTime()) / 1000;
        assertThat(durationSeconds).isBetween(
                (long) SESSION_EXPIRATION_SECONDS - 1,
                (long) SESSION_EXPIRATION_SECONDS + 1);
    }

    private void assertSessionColumns(Map<String, Object> session) {
        assertThat(session).containsKey("id");
        assertThat(session).containsKey("token_hash");
        assertThat(session).containsKey("user_id");
        assertThat(session).containsKey("created_at");
        assertThat(session).containsKey("expires_at");
        assertThat(session).hasSize(5);
    }
}

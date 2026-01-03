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
        // Step 1: Register
        log.info("Registering user: {}", USERNAME);
        RegisterRequest registerRequest = new RegisterRequest(USERNAME, PASSWORD);

        var registerResult = restTestClient.post()
                .uri("/api/v1/auth/register")
                .body(registerRequest)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class);

        String registerBody = registerResult.getResponseBody();
        RegisterResponse registerResponse = objectMapper.readValue(registerBody, RegisterResponse.class);

        Instant now = Instant.now();
        assertThat(registerResponse.getUserId()).isEqualTo(EXPECTED_USER_ID);
        assertThat(registerResponse.getUsername()).isEqualTo(USERNAME);
        assertTimestampsAreClose(registerResponse.getCreatedAt(), now);
        assertTimestampsAreClose(registerResponse.getUpdatedAt(), now);

        // Step 2: First Login
        log.info("First login for user: {}", USERNAME);
        LoginRequest loginRequest = new LoginRequest(USERNAME, PASSWORD);

        var loginResult1 = restTestClient.post()
                .uri("/api/v1/auth/login")
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String loginBody1 = loginResult1.getResponseBody();
        LoginResponse loginResponse1 = objectMapper.readValue(loginBody1, LoginResponse.class);

        now = Instant.now();
        String token1 = loginResponse1.getToken();
        assertThat(token1).isNotBlank();
        assertThat(loginResponse1.getUserId()).isEqualTo(EXPECTED_USER_ID);
        assertThat(loginResponse1.getUsername()).isEqualTo(USERNAME);
        assertThat(loginResponse1.getTokenType()).isEqualTo("Bearer");
        assertThat(loginResponse1.getExpiresIn()).isEqualTo(SESSION_EXPIRATION_SECONDS);
        assertTimestampsAreClose(loginResponse1.getCreatedAt(), now);
        assertTimestampsAreClose(loginResponse1.getExpiresAt(), now.plusSeconds(SESSION_EXPIRATION_SECONDS));

        // Step 3: Validate first token
        log.info("Validating first token");
        var validateResult1 = restTestClient.get()
                .uri("/api/v1/auth/validate")
                .header("Authorization", "Bearer: " + token1)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String validateBody1 = validateResult1.getResponseBody();
        ValidateResponse validateResponse1 = objectMapper.readValue(validateBody1, ValidateResponse.class);

        assertThat(validateResponse1.isValid()).isTrue();
        assertThat(validateResponse1.getUserId()).isEqualTo(EXPECTED_USER_ID);
        assertThat(validateResponse1.getUsername()).isEqualTo(USERNAME);
        assertThat(validateResponse1.getCreatedAt()).isEqualTo(loginResponse1.getCreatedAt());
        assertThat(validateResponse1.getExpiresAt()).isEqualTo(loginResponse1.getExpiresAt());

        // Step 4: Second Login (creating another session)
        log.info("Second login for user: {}", USERNAME);
        var loginResult2 = restTestClient.post()
                .uri("/api/v1/auth/login")
                .body(loginRequest)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        String loginBody2 = loginResult2.getResponseBody();
        LoginResponse loginResponse2 = objectMapper.readValue(loginBody2, LoginResponse.class);

        now = Instant.now();
        String token2 = loginResponse2.getToken();
        assertThat(token2).isNotBlank();
        assertThat(token2).isNotEqualTo(token1);
        assertThat(loginResponse2.getUserId()).isEqualTo(EXPECTED_USER_ID);
        assertThat(loginResponse2.getUsername()).isEqualTo(USERNAME);
        assertTimestampsAreClose(loginResponse2.getCreatedAt(), now);
        assertTimestampsAreClose(loginResponse2.getExpiresAt(), now.plusSeconds(SESSION_EXPIRATION_SECONDS));

        // Step 5: Validate BOTH tokens are still valid (multiple sessions supported)
        log.info("Validating both tokens are still valid");
        var validateResultToken1Again = restTestClient.get()
                .uri("/api/v1/auth/validate")
                .header("Authorization", "Bearer: " + token1)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        ValidateResponse validateToken1Again = objectMapper.readValue(
                validateResultToken1Again.getResponseBody(), ValidateResponse.class);
        assertThat(validateToken1Again.isValid()).isTrue();

        var validateResultToken2 = restTestClient.get()
                .uri("/api/v1/auth/validate")
                .header("Authorization", "Bearer: " + token2)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        ValidateResponse validateToken2 = objectMapper.readValue(
                validateResultToken2.getResponseBody(), ValidateResponse.class);
        assertThat(validateToken2.isValid()).isTrue();

        // Step 6: Verify database state using JDBC
        log.info("Verifying database state");
        verifyDatabaseState(token1, token2);

        log.info("Full authentication flow completed successfully");
    }

    private void verifyDatabaseState(String token1, String token2) {
        // Verify users table
        log.info("Verifying users table");
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");
        assertThat(users).hasSize(1);

        Map<String, Object> user = users.getFirst();
        assertThat(user).containsKey("id");
        assertThat(user).containsKey("username");
        assertThat(user).containsKey("normalized_username");
        assertThat(user).containsKey("password_hash");
        assertThat(user).containsKey("created_at");
        assertThat(user).containsKey("updated_at");
        assertThat(user).hasSize(6);

        assertThat(user.get("id")).isEqualTo(EXPECTED_USER_ID);
        assertThat(user.get("username")).isEqualTo(USERNAME);
        assertThat(user.get("normalized_username")).isEqualTo(USERNAME_NORMALIZED);

        // Verify password hash matches the original password
        String storedPasswordHash = (String) user.get("password_hash");
        assertThat(storedPasswordHash).isNotBlank();
        assertThat(passwordEncoder.matches(PASSWORD, storedPasswordHash)).isTrue();

        Timestamp userCreatedAt = (Timestamp) user.get("created_at");
        Timestamp userUpdatedAt = (Timestamp) user.get("updated_at");
        assertThat(userCreatedAt).isNotNull();
        assertThat(userUpdatedAt).isNotNull();

        // Verify sessions table
        log.info("Verifying sessions table");
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList("SELECT * FROM sessions ORDER BY id");
        assertThat(sessions).hasSize(2);

        Map<String, Object> session1 = sessions.get(0);
        assertThat(session1).containsKey("id");
        assertThat(session1).containsKey("token_hash");
        assertThat(session1).containsKey("user_id");
        assertThat(session1).containsKey("created_at");
        assertThat(session1).containsKey("expires_at");
        assertThat(session1).hasSize(5);

        // Verify first session token hash exactly
        String expectedToken1Hash = TestDataFactory.hashToken(token1);
        assertThat(session1.get("token_hash")).isEqualTo(expectedToken1Hash);
        assertThat(session1.get("user_id")).isEqualTo(EXPECTED_USER_ID);

        Timestamp session1CreatedAt = (Timestamp) session1.get("created_at");
        Timestamp session1ExpiresAt = (Timestamp) session1.get("expires_at");
        assertThat(session1CreatedAt).isNotNull();
        assertThat(session1ExpiresAt).isNotNull();
        long session1DurationSeconds = (session1ExpiresAt.getTime() - session1CreatedAt.getTime()) / 1000;
        assertThat(session1DurationSeconds).isBetween(
                (long) SESSION_EXPIRATION_SECONDS - 1, (long) SESSION_EXPIRATION_SECONDS + 1);

        // Verify second session token hash exactly
        Map<String, Object> session2 = sessions.get(1);
        String expectedToken2Hash = TestDataFactory.hashToken(token2);
        assertThat(session2.get("token_hash")).isEqualTo(expectedToken2Hash);
        assertThat(session2.get("user_id")).isEqualTo(EXPECTED_USER_ID);

        Timestamp session2CreatedAt = (Timestamp) session2.get("created_at");
        Timestamp session2ExpiresAt = (Timestamp) session2.get("expires_at");
        assertThat(session2CreatedAt).isNotNull();
        assertThat(session2ExpiresAt).isNotNull();
        long session2DurationSeconds = (session2ExpiresAt.getTime() - session2CreatedAt.getTime()) / 1000;
        assertThat(session2DurationSeconds).isBetween(
                (long) SESSION_EXPIRATION_SECONDS - 1, (long) SESSION_EXPIRATION_SECONDS + 1);

        // Verify sessions have different token hashes
        assertThat(session1.get("token_hash")).isNotEqualTo(session2.get("token_hash"));

        log.info("Database verification completed: 1 user, 2 sessions");
    }

    private void assertTimestampsAreClose(Instant actual, Instant expected) {
        assertThat(actual).isNotNull();
        long diffSeconds = Math.abs(Duration.between(actual, expected).toSeconds());
        assertThat(diffSeconds)
                .as("Timestamp %s should be within 1 second of %s", actual, expected)
                .isLessThanOrEqualTo(1);
    }
}

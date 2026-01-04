package com.onlineshop.auth.component.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.ErrorResponse;
import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.LoginResponse;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.dto.RegisterResponse;
import com.onlineshop.auth.dto.ValidateResponse;
import com.onlineshop.auth.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureRestTestClient
class ComponentIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ComponentIT.class);

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String REGISTER_PATH = AUTH_BASE_PATH + "/register";
    private static final String LOGIN_PATH = AUTH_BASE_PATH + "/login";
    private static final String VALIDATE_PATH = AUTH_BASE_PATH + "/validate";

    private static final String USERNAME = "TestUser";
    private static final String USERNAME_NORMALIZED = "testuser";
    private static final String PASSWORD = "password123";
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
        Long userId = registerResponse.getUserId();
        assertRegisterResponse(registerResponse, userId, USERNAME);

        // First login
        LoginResponse firstLogin = login(USERNAME, PASSWORD);
        assertLoginResponse(firstLogin, userId, USERNAME);

        // Validate first token
        ValidateResponse firstValidation = validateToken(firstLogin.getToken());
        assertValidateResponse(firstValidation, userId, USERNAME, firstLogin);

        // Second login (creating another session)
        LoginResponse secondLogin = login(USERNAME, PASSWORD);
        assertLoginResponse(secondLogin, userId, USERNAME);
        assertThat(secondLogin.getToken()).isNotEqualTo(firstLogin.getToken());

        // Both tokens should still be valid (multiple sessions supported)
        assertTokenIsValid(firstLogin.getToken());
        assertTokenIsValid(secondLogin.getToken());

        // Verify database state
        verifyUserInDatabase(userId, USERNAME, USERNAME_NORMALIZED, PASSWORD);
        verifySessionsInDatabase(userId, firstLogin.getToken(), secondLogin.getToken());

        log.info("Full authentication flow completed successfully");
    }

    @Test
    void login_withWrongPassword_returnsUnauthorized() throws Exception {
        // First register a user
        registerUser(USERNAME, PASSWORD);

        // Try to login with wrong password
        log.info("Attempting login with wrong password");
        ErrorResponse errorResponse = postRequestExpectingError(
                LOGIN_PATH,
                new LoginRequest(USERNAME, "wrongPassword"),
                HttpStatus.UNAUTHORIZED
        );

        assertErrorResponse(
                errorResponse,
                HttpStatus.UNAUTHORIZED,
                "https://api.onlineshop.com/errors/invalid-username-or-password",
                "Unauthorized",
                LOGIN_PATH
        );
    }

    @Test
    void request_toNonExistentEndpoint_returnsNotFound() throws Exception {
        String nonExistentPath = AUTH_BASE_PATH + "/nonexistent";
        log.info("Requesting non-existent endpoint");
        var result = restTestClient.get()
                .uri(nonExistentPath)
                .exchange()
                .expectStatus().isNotFound()
                .returnResult(String.class);

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponseBody(), ErrorResponse.class);

        assertErrorResponse(
                errorResponse,
                HttpStatus.NOT_FOUND,
                "https://api.onlineshop.com/errors/not-found",
                "Not Found",
                nonExistentPath
        );
    }

    @ParameterizedTest(name = "{0} {1} should return 405 Method Not Allowed")
    @MethodSource("unsupportedMethodsProvider")
    void request_withUnsupportedMethod_returnsMethodNotAllowed(HttpMethod method, String endpoint) throws Exception {
        log.info("Testing unsupported method {} on endpoint {}", method, endpoint);

        var result = restTestClient.method(method)
                .uri(endpoint)
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.METHOD_NOT_ALLOWED)
                .returnResult(String.class);

        ErrorResponse errorResponse = objectMapper.readValue(result.getResponseBody(), ErrorResponse.class);

        assertErrorResponse(
                errorResponse,
                HttpStatus.METHOD_NOT_ALLOWED,
                "https://api.onlineshop.com/errors/method-not-allowed",
                "Method Not Allowed",
                endpoint
        );
    }

    private static Stream<Arguments> unsupportedMethodsProvider() {
        return Stream.of(
                // REGISTER_PATH - only POST is allowed
                Arguments.of(HttpMethod.GET, REGISTER_PATH),
                Arguments.of(HttpMethod.PUT, REGISTER_PATH),
                Arguments.of(HttpMethod.DELETE, REGISTER_PATH),
                Arguments.of(HttpMethod.PATCH, REGISTER_PATH),

                // LOGIN_PATH - only POST is allowed
                Arguments.of(HttpMethod.GET, LOGIN_PATH),
                Arguments.of(HttpMethod.PUT, LOGIN_PATH),
                Arguments.of(HttpMethod.DELETE, LOGIN_PATH),
                Arguments.of(HttpMethod.PATCH, LOGIN_PATH),

                // VALIDATE_PATH - only GET is allowed
                Arguments.of(HttpMethod.POST, VALIDATE_PATH),
                Arguments.of(HttpMethod.PUT, VALIDATE_PATH),
                Arguments.of(HttpMethod.DELETE, VALIDATE_PATH),
                Arguments.of(HttpMethod.PATCH, VALIDATE_PATH)
        );
    }

    // ==================== Token Validation Error Tests ====================

    @Test
    void validate_withMissingBearerPrefix_returnsUnauthorized() throws Exception {
        log.info("Testing token validation without Bearer prefix");

        ErrorResponse errorResponse = validateTokenExpectingError("some-token-without-bearer-prefix");

        assertErrorResponse(
                errorResponse,
                HttpStatus.UNAUTHORIZED,
                "https://api.onlineshop.com/errors/invalid-token",
                "Unauthorized",
                VALIDATE_PATH
        );
    }

    @Test
    void validate_withInvalidRandomToken_returnsUnauthorized() throws Exception {
        log.info("Testing token validation with invalid random token");

        // A properly formatted but non-existent/invalid token
        String randomToken = "abc123-invalid-random-token-xyz789";
        ErrorResponse errorResponse = validateTokenExpectingError("Bearer: " + randomToken);

        assertErrorResponse(
                errorResponse,
                HttpStatus.UNAUTHORIZED,
                "https://api.onlineshop.com/errors/invalid-token",
                "Unauthorized",
                VALIDATE_PATH
        );
    }

    // ==================== Username Uniqueness Tests ====================

    /**
     * Verifies that username uniqueness is case-insensitive.
     * When a user registers with "TestUser", another registration with "TESTUSER" should fail.
     */
    @Test
    void register_withSameUsernameDifferentCasing_returnsConflict() throws Exception {
        // First registration succeeds
        String testUser = "TestUser";
        registerUser(testUser, PASSWORD);

        // Second registration with different casing should fail
        ErrorResponse errorResponse = postRequestExpectingError(
                REGISTER_PATH,
                new RegisterRequest("TESTUSER", PASSWORD),
                HttpStatus.CONFLICT
        );

        assertErrorResponse(
                errorResponse,
                HttpStatus.CONFLICT,
                "https://api.onlineshop.com/errors/user-already-exists",
                "Conflict",
                REGISTER_PATH
        );

        // Verify only one user exists in database
        List<Map<String, Object>> users = jdbcTemplate.queryForList("SELECT * FROM users");
        assertThat(users).as("Only one user should exist in database").hasSize(1);
        assertThat(users.get(0).get("normalized_username")).isEqualTo("testuser");
        assertThat(users.get(0).get("username")).isEqualTo(testUser);
    }

    // ==================== HTTP Request Helpers ====================

    private RegisterResponse registerUser(String username, String password) throws Exception {
        log.info("Registering user: {}", username);
        return postRequest(
                REGISTER_PATH,
                new RegisterRequest(username, password),
                RegisterResponse.class,
                HttpStatus.CREATED
        );
    }

    private LoginResponse login(String username, String password) throws Exception {
        log.info("Logging in user: {}", username);
        return postRequest(
                LOGIN_PATH,
                new LoginRequest(username, password),
                LoginResponse.class,
                HttpStatus.OK
        );
    }

    private ValidateResponse validateToken(String token) throws Exception {
        log.info("Validating token");
        var result = restTestClient.get()
                .uri(VALIDATE_PATH)
                .header("Authorization", "Bearer: " + token)
                .exchange()
                .expectStatus().isOk()
                .returnResult(String.class);

        return objectMapper.readValue(result.getResponseBody(), ValidateResponse.class);
    }

    private ErrorResponse validateTokenExpectingError(String authHeader) throws Exception {
        log.info("Validating token expecting error");
        var result = restTestClient.get()
                .uri(VALIDATE_PATH)
                .header("Authorization", authHeader)
                .exchange()
                .expectStatus().isUnauthorized()
                .returnResult(String.class);

        return objectMapper.readValue(result.getResponseBody(), ErrorResponse.class);
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

    private ErrorResponse postRequestExpectingError(String uri, Object body, HttpStatus expectedStatus) throws Exception {
        var result = restTestClient.post()
                .uri(uri)
                .body(body)
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .returnResult(String.class);

        return objectMapper.readValue(result.getResponseBody(), ErrorResponse.class);
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

    private void assertErrorResponse(ErrorResponse response, HttpStatus expectedStatus,
                                      String expectedType, String expectedTitle, String expectedInstance) {
        assertThat(response.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(response.getType()).isEqualTo(expectedType);
        assertThat(response.getTitle()).isEqualTo(expectedTitle);
        assertThat(response.getInstance()).isEqualTo(expectedInstance);
        assertThat(response.getDetail()).isNotBlank();
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

    private void verifySessionsInDatabase(Long expectedUserId, String token1, String token2) {
        log.info("Verifying sessions table");
        List<Map<String, Object>> sessions = jdbcTemplate.queryForList("SELECT * FROM sessions ORDER BY id");
        assertThat(sessions).hasSize(2);

        verifySession(sessions.get(0), expectedUserId, token1);
        verifySession(sessions.get(1), expectedUserId, token2);

        assertThat(sessions.get(0).get("token_hash"))
                .isNotEqualTo(sessions.get(1).get("token_hash"));

        log.info("Database verification completed: 1 user, 2 sessions");
    }

    private void verifySession(Map<String, Object> session, Long expectedUserId, String token) {
        assertSessionColumns(session);

        String expectedTokenHash = TestDataFactory.hashToken(token);
        assertThat(session.get("token_hash")).isEqualTo(expectedTokenHash);
        assertThat(session.get("user_id")).isEqualTo(expectedUserId);

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

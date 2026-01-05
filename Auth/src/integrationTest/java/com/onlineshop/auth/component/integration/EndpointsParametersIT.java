package com.onlineshop.auth.component.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.ErrorResponse;
import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.RegisterRequest;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

@AutoConfigureRestTestClient
class EndpointsParametersIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(EndpointsParametersIT.class);

    private static final String AUTH_BASE_PATH = "/api/v1/auth";
    private static final String REGISTER_PATH = AUTH_BASE_PATH + "/register";
    private static final String LOGIN_PATH = AUTH_BASE_PATH + "/login";
    private static final String VALIDATE_PATH = AUTH_BASE_PATH + "/validate";

    @Autowired
    private RestTestClient restTestClient;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    // ==================== Register Endpoint Validation Tests ====================

    @ParameterizedTest(name = "Register: {0}")
    @MethodSource("invalidRegisterRequestsProvider")
    void register_withInvalidInput_returnsBadRequest(
            String testName,
            String username,
            String password,
            List<String> expectedFieldsInError) throws Exception {

        log.info("Testing register: {}", testName);

        RegisterRequest request = new RegisterRequest(username, password);
        ErrorResponse errorResponse = postRequestExpectingError(
                REGISTER_PATH,
                request,
                HttpStatus.BAD_REQUEST
        );

        assertValidationErrorResponse(errorResponse, REGISTER_PATH);
        for (String field : expectedFieldsInError) {
            assertThat(errorResponse.getDetail()).contains(field);
        }
    }

    // ==================== Login Endpoint Validation Tests ====================

    @ParameterizedTest(name = "Login: {0}")
    @MethodSource("invalidLoginRequestsProvider")
    void login_withInvalidInput_returnsBadRequest(
            String testName,
            String username,
            String password,
            List<String> expectedFieldsInError) throws Exception {

        log.info("Testing login: {}", testName);

        LoginRequest request = new LoginRequest(username, password);
        ErrorResponse errorResponse = postRequestExpectingError(
                LOGIN_PATH,
                request,
                HttpStatus.BAD_REQUEST
        );

        assertValidationErrorResponse(errorResponse, LOGIN_PATH);
        for (String field : expectedFieldsInError) {
            assertThat(errorResponse.getDetail()).contains(field);
        }
    }

    // ==================== Validate Endpoint Tests ====================

    @ParameterizedTest(name = "Validate: {0}")
    @MethodSource("invalidValidateHeadersProvider")
    void validate_withInvalidAuthHeader_returnsUnauthorized(
            String testName,
            String authHeader,
            HttpStatus expectedStatus) throws Exception {

        log.info("Testing validate: {}", testName);

        var requestSpec = restTestClient.get().uri(VALIDATE_PATH);

        if (authHeader != null) {
            requestSpec.header("Authorization", authHeader);
        }

        var result = requestSpec
                .exchange()
                .expectStatus().isEqualTo(expectedStatus)
                .returnResult(String.class);

        ErrorResponse errorResponse = objectMapper.readValue(
                result.getResponseBody(),
                ErrorResponse.class
        );

        assertThat(errorResponse.getStatus()).isEqualTo(expectedStatus.value());
        assertThat(errorResponse.getInstance()).isEqualTo(VALIDATE_PATH);
    }

    // ==================== Test Data Providers ====================

    private static Stream<Arguments> invalidRegisterRequestsProvider() {
        return Stream.of(
                // Single field invalid - username
                arguments("null username", null, "validPassword123", List.of("username")),
                arguments("empty username", "", "validPassword123", List.of("username")),
                arguments("blank username", "   ", "validPassword123", List.of("username")),
                arguments("whitespace-only username", "\t\n", "validPassword123", List.of("username")),

                // Single field invalid - password
                arguments("null password", "validUsername", null, List.of("password")),
                arguments("empty password", "validUsername", "", List.of("password")),
                arguments("blank password", "validUsername", "   ", List.of("password")),
                arguments("whitespace-only password", "validUsername", "\t\n", List.of("password")),

                // Both fields invalid
                arguments("both fields null", null, null, List.of("username", "password")),
                arguments("both fields empty", "", "", List.of("username", "password")),
                arguments("both fields blank", "   ", "   ", List.of("username", "password")),
                arguments("both fields whitespace-only", "\t\n", "\t\n", List.of("username", "password"))
        );
    }

    private static Stream<Arguments> invalidLoginRequestsProvider() {
        return Stream.of(
                // Single field invalid - username
                arguments("null username", null, "validPassword123", List.of("username")),
                arguments("empty username", "", "validPassword123", List.of("username")),
                arguments("blank username", "   ", "validPassword123", List.of("username")),
                arguments("whitespace-only username", "\t\n", "validPassword123", List.of("username")),

                // Single field invalid - password
                arguments("null password", "validUsername", null, List.of("password")),
                arguments("empty password", "validUsername", "", List.of("password")),
                arguments("blank password", "validUsername", "   ", List.of("password")),
                arguments("whitespace-only password", "validUsername", "\t\n", List.of("password")),

                // Both fields invalid
                arguments("both fields null", null, null, List.of("username", "password")),
                arguments("both fields empty", "", "", List.of("username", "password")),
                arguments("both fields blank", "   ", "   ", List.of("username", "password")),
                arguments("both fields whitespace-only", "\t\n", "\t\n", List.of("username", "password"))
        );
    }

    private static Stream<Arguments> invalidValidateHeadersProvider() {
        return Stream.of(
                arguments("missing Authorization header", null, HttpStatus.UNAUTHORIZED),
                arguments("empty Authorization header", "", HttpStatus.UNAUTHORIZED),
                arguments("invalid format - no Bearer prefix", "some-token", HttpStatus.UNAUTHORIZED),
                arguments("invalid format - Bearer without colon", "Bearer token123", HttpStatus.UNAUTHORIZED),
                arguments("empty token after Bearer:", "Bearer: ", HttpStatus.UNAUTHORIZED),
                arguments("blank token after Bearer:", "Bearer:    ", HttpStatus.UNAUTHORIZED),
                arguments("invalid token", "Bearer: invalid-token-12345", HttpStatus.UNAUTHORIZED)
        );
    }

    // ==================== HTTP Request Helpers ====================

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

    private void assertValidationErrorResponse(ErrorResponse response, String expectedInstance) {
        assertThat(response.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getType()).isEqualTo("https://api.onlineshop.com/errors/validation-failed");
        assertThat(response.getTitle()).isEqualTo("Bad Request");
        assertThat(response.getInstance()).isEqualTo(expectedInstance);
        assertThat(response.getDetail()).isNotBlank();
        assertThat(response.getDetail()).contains("Validation failed:");
    }
}

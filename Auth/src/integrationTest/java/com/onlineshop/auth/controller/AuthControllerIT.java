package com.onlineshop.auth.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.LoginRequest;
import com.onlineshop.auth.dto.LoginResponse;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.dto.RegisterResponse;
import com.onlineshop.auth.dto.ValidateResponse;
import com.onlineshop.auth.entity.Session;
import com.onlineshop.auth.entity.User;
import com.onlineshop.auth.testutil.TestDataFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthControllerIT extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void register_whenValidRequest_returns201WithUserDetails() throws Exception {
        RegisterRequest request = TestDataFactory.createRegisterRequest("newuser", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.userId").isNumber())
                .andExpect(jsonPath("$.username").value("newuser"));
    }

    @Test
    void register_whenUsernameExists_returns409Conflict() throws Exception {
        User existingUser = TestDataFactory.createUser("existinguser", passwordEncoder.encode("pass"));
        userRepository.save(existingUser);

        RegisterRequest request = TestDataFactory.createRegisterRequest("existinguser", "password123");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_passwordIsHashedInDatabase() throws Exception {
        RegisterRequest request = TestDataFactory.createRegisterRequest("hashuser", "plaintextpassword");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        Optional<User> savedUser = userRepository.findByNormalizedUsername("hashuser");
        assertThat(savedUser).isPresent();
        assertThat(savedUser.get().getPasswordHash()).isNotEqualTo("plaintextpassword");
        assertThat(passwordEncoder.matches("plaintextpassword", savedUser.get().getPasswordHash())).isTrue();
    }

    @Test
    void login_whenValidCredentials_returns200WithToken() throws Exception {
        User user = TestDataFactory.createUser("loginuser", passwordEncoder.encode("correctpassword"));
        userRepository.save(user);

        LoginRequest request = TestDataFactory.createLoginRequest("loginuser", "correctpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void login_whenUserNotFound_returns401Unauthorized() throws Exception {
        LoginRequest request = TestDataFactory.createLoginRequest("nonexistent", "password");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_whenWrongPassword_returns401Unauthorized() throws Exception {
        User user = TestDataFactory.createUser("wrongpassuser", passwordEncoder.encode("correctpassword"));
        userRepository.save(user);

        LoginRequest request = TestDataFactory.createLoginRequest("wrongpassuser", "wrongpassword");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_createsSessionInDatabase() throws Exception {
        User user = TestDataFactory.createUser("sessionuser", passwordEncoder.encode("password"));
        userRepository.save(user);

        LoginRequest request = TestDataFactory.createLoginRequest("sessionuser", "password");

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse response = objectMapper.readValue(
                result.getResponse().getContentAsString(), LoginResponse.class);

        String tokenHash = TestDataFactory.hashToken(response.getToken());
        Optional<Session> session = sessionRepository.findByTokenHash(tokenHash);
        assertThat(session).isPresent();
        assertThat(session.get().getUser().getId()).isEqualTo(user.getId());
    }

    @Test
    void validate_whenValidToken_returns200WithUserInfo() throws Exception {
        User user = userRepository.save(TestDataFactory.createUser("validateuser", "hash"));
        String token = "valid-token-123";
        String tokenHash = TestDataFactory.hashToken(token);
        Session session = TestDataFactory.createSession(tokenHash, user, Instant.now().plus(1, ChronoUnit.HOURS));
        sessionRepository.save(session);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer: " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value(user.getId()))
                .andExpect(jsonPath("$.username").value("validateuser"));
    }

    @Test
    void validate_whenTokenNotFound_returns401Unauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer: invalid-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_whenTokenExpired_returns401Unauthorized() throws Exception {
        User user = userRepository.save(TestDataFactory.createUser("expireduser", "hash"));
        String token = "expired-token-456";
        String tokenHash = TestDataFactory.hashToken(token);
        Session session = TestDataFactory.createSession(tokenHash, user, Instant.now().minus(1, ChronoUnit.HOURS));
        sessionRepository.save(session);

        mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer: " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void fullAuthenticationFlow_registerLoginValidate_succeeds() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = TestDataFactory.createRegisterRequest("flowuser", "flowpassword");
        MvcResult registerResult = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andReturn();

        RegisterResponse registerResponse = objectMapper.readValue(
                registerResult.getResponse().getContentAsString(), RegisterResponse.class);
        assertThat(registerResponse.getUsername()).isEqualTo("flowuser");

        // Step 2: Login
        LoginRequest loginRequest = TestDataFactory.createLoginRequest("flowuser", "flowpassword");
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginRequest)))
                .andExpect(status().isOk())
                .andReturn();

        LoginResponse loginResponse = objectMapper.readValue(
                loginResult.getResponse().getContentAsString(), LoginResponse.class);
        assertThat(loginResponse.getToken()).isNotBlank();

        // Step 3: Validate
        MvcResult validateResult = mockMvc.perform(get("/api/v1/auth/validate")
                        .header("Authorization", "Bearer: " + loginResponse.getToken()))
                .andExpect(status().isOk())
                .andReturn();

        ValidateResponse validateResponse = objectMapper.readValue(
                validateResult.getResponse().getContentAsString(), ValidateResponse.class);
        assertThat(validateResponse.getUsername()).isEqualTo("flowuser");
        assertThat(validateResponse.getUserId()).isEqualTo(registerResponse.getUserId());
    }
}

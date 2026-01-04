package com.onlineshop.auth.component.integration;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.ErrorResponse;
import com.onlineshop.auth.dto.RegisterRequest;
import com.onlineshop.auth.dto.RegisterResponse;
import com.onlineshop.auth.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

/**
 * Tests for concurrent registration race conditions.
 *
 * This test simulates the scenario where two threads simultaneously pass the
 * existsByNormalizedUsername check, and one of them fails when saving due to
 * the database unique constraint. It verifies that the system properly handles
 * this race condition by returning 409 Conflict instead of 500 Internal Server Error.
 */
@AutoConfigureRestTestClient
class ConcurrentRegistrationIT extends BaseIntegrationTest {

    private static final Logger log = LoggerFactory.getLogger(ConcurrentRegistrationIT.class);

    private static final String REGISTER_PATH = "/api/v1/auth/register";
    private static final String USERNAME = "RaceConditionUser";
    private static final String PASSWORD = "password123";

    @Autowired
    private RestTestClient restTestClient;

    @MockitoSpyBean
    private UserRepository spiedUserRepository;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

    /**
     * Simulates a race condition where two concurrent registration requests for the
     * same username both pass the existsByNormalizedUsername check, but one fails
     * when saving due to the database unique constraint.
     *
     * Expected behavior: One request succeeds with 201 Created, the other returns
     * 409 Conflict (not 500 Internal Server Error).
     */
    @Test
    void concurrentRegistration_whenBothThreadsPassExistsCheck_oneSucceedsAndOtherReturnsConflict() throws Exception {
        // Latch to synchronize both threads at the exists check
        // When both threads call countDown(), both await() calls unblock simultaneously
        CountDownLatch bothThreadsReachedCheck = new CountDownLatch(2);

        // Spy on existsByNormalizedUsername to coordinate both threads
        // Since UserRepository is an interface, we can't use callRealMethod().
        // Instead, we return false (user doesn't exist) which is correct for both threads
        // at the moment they check - the race condition occurs when both proceed to save.
        doAnswer((Answer<Boolean>) invocation -> {
            log.info("Thread {} reached existsByNormalizedUsername check", Thread.currentThread().getName());

            // Signal this thread reached the check, then wait for the other thread
            bothThreadsReachedCheck.countDown();
            boolean bothReached = bothThreadsReachedCheck.await(5, TimeUnit.SECONDS);
            if (!bothReached) {
                throw new RuntimeException("Timeout waiting for both threads to reach exists check");
            }

            // Return false - both threads now proceed to save() simultaneously
            log.info("Thread {} existsByNormalizedUsername returning false", Thread.currentThread().getName());
            return false;
        }).when(spiedUserRepository).existsByNormalizedUsername(anyString());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            // Submit two concurrent registration requests
            Future<HttpResponseResult> future1 = executor.submit(() -> registerAndCaptureResponse(USERNAME.toUpperCase(), PASSWORD));
            Future<HttpResponseResult> future2 = executor.submit(() -> registerAndCaptureResponse(USERNAME.toLowerCase(), PASSWORD));

            // Get results
            HttpResponseResult result1 = future1.get(10, TimeUnit.SECONDS);
            HttpResponseResult result2 = future2.get(10, TimeUnit.SECONDS);

            log.info("Thread 1 result: {} - {}", result1.statusCode(), result1.body());
            log.info("Thread 2 result: {} - {}", result2.statusCode(), result2.body());

            // Verify outcomes: one should succeed (201), one should fail with conflict (409)
            assertOneSucceedsOneConflicts(result1, result2);

        } finally {
            executor.shutdownNow();
        }
    }

    private HttpResponseResult registerAndCaptureResponse(String username, String password) {
        try {
            log.info("Thread {} starting registration for user: {}", Thread.currentThread().getName(), username);

            var result = restTestClient.post()
                    .uri(REGISTER_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new RegisterRequest(username, password))
                    .exchange()
                    .returnResult(String.class);

            int statusCode = result.getStatus().value();
            String body = result.getResponseBody();

            log.info("Thread {} completed with status: {}", Thread.currentThread().getName(), statusCode);
            return new HttpResponseResult(statusCode, body);
        } catch (Exception e) {
            log.error("Thread {} failed with exception", Thread.currentThread().getName(), e);
            return new HttpResponseResult(500, "Exception: " + e.getMessage());
        }
    }

    private void assertOneSucceedsOneConflicts(HttpResponseResult result1, HttpResponseResult result2) throws Exception {
        HttpResponseResult successResult;
        HttpResponseResult conflictResult;

        if (result1.statusCode() == HttpStatus.CREATED.value()) {
            successResult = result1;
            conflictResult = result2;
        } else if (result2.statusCode() == HttpStatus.CREATED.value()) {
            successResult = result2;
            conflictResult = result1;
        } else {
            // Neither succeeded - this is a failure
            throw new AssertionError(String.format(
                    "Expected one request to succeed with 201, but got status codes: %d and %d. " +
                    "Bodies: [%s] and [%s]",
                    result1.statusCode(), result2.statusCode(), result1.body(), result2.body()));
        }

        // Verify the successful response
        assertThat(successResult.statusCode())
                .as("One request should succeed with 201 Created")
                .isEqualTo(HttpStatus.CREATED.value());

        RegisterResponse registerResponse = objectMapper.readValue(successResult.body(), RegisterResponse.class);
        assertThat(registerResponse.getUsername()).isEqualTo(USERNAME);
        assertThat(registerResponse.getUserId()).isNotNull();

        // Verify the conflict response - THIS IS THE KEY ASSERTION
        assertThat(conflictResult.statusCode())
                .as("The other request should return 409 Conflict (not 500 Internal Server Error)")
                .isEqualTo(HttpStatus.CONFLICT.value());

        ErrorResponse errorResponse = objectMapper.readValue(conflictResult.body(), ErrorResponse.class);
        assertThat(errorResponse.getType()).isEqualTo("https://api.onlineshop.com/errors/user-already-exists");
        assertThat(errorResponse.getTitle()).isEqualTo("Conflict");
        assertThat(errorResponse.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(errorResponse.getInstance()).isEqualTo(REGISTER_PATH);

        log.info("Race condition test passed: one succeeded with 201, one properly returned 409");
    }

    private record HttpResponseResult(int statusCode, String body) {}
}

# Change Validate Endpoint Behavior - Implementation Plan

**Date:** 2026-01-10
**Feature:** Modify token validation endpoint to return 200 OK for both valid and invalid tokens

---

## Overview

Change the `/api/v1/auth/validate` endpoint behavior:
- **Current:** Throws exceptions (returns 401) for invalid/expired tokens
- **New:** Returns 200 OK with `valid: false` for invalid/expired tokens

### Implementation Summary

**Total Phases:** 8
**Total Tasks:** 38
- Phase 1: Service Layer (1 task)
- Phase 2: Controller Layer (3 tasks)
- Phase 3: Exception Handlers (5 tasks)
- Phase 4: Unit Tests (3 tasks)
- Phase 5: Update Existing Integration Tests (5 tasks)
- Phase 6: Add New Integration Tests (6 tasks)
- Phase 7: Remove Redundant Code (7 tasks)
- Phase 8: Build & Verify (4 tasks)

---

## Requirements

### Status Code Behavior

| Scenario | Current Behavior | New Behavior |
|----------|------------------|--------------|
| Missing Authorization header | 401 Unauthorized | **400 Bad Request** |
| Empty Authorization header | 401 Unauthorized | **401 Unauthorized** |
| Invalid token format (no "Bearer: " prefix) | 401 Unauthorized | **401 Unauthorized** |
| Token not found in database | 401 Unauthorized | **200 OK** with `valid: false` |
| Token expired | 401 Unauthorized | **200 OK** with `valid: false` |
| Valid token | 200 OK with user data | **200 OK** with user data |

### Response Format

**Valid token response:**
```json
{
  "valid": true,
  "userId": 1,
  "username": "testuser",
  "createdAt": "2026-01-10T10:00:00Z",
  "expiresAt": "2026-01-10T11:00:00Z"
}
```

**Invalid token response:**
```json
{
  "valid": false
}
```

---

Use the same DTO for both - valid and invalid tokens (just skip nulls in case of invalid)

## Quick Reference: What Changes in Each Phase

| Phase | Focus | Key Changes |
|-------|-------|-------------|
| **Phase 1** | Service Logic | Return `valid: false` instead of throwing exceptions for invalid tokens |
| **Phase 2** | Controller | Distinguish between missing (400) and invalid format (401) errors |
| **Phase 3** | Exception Handlers | Add new handlers for `InvalidTokenFormatException` and `MissingAuthorizationHeaderException` |
| **Phase 4** | Unit Tests | Update 3 tests to expect response instead of exception |
| **Phase 5** | Existing Integration Tests | Update 5 tests for new status codes and behaviors |
| **Phase 6** | New Integration Tests | Add 6 comprehensive tests covering all edge cases |
| **Phase 7** | Code Cleanup | Remove unused imports, redundant code, obsolete handlers |
| **Phase 8** | Verification | Build, test, verify coverage |

---

## Tasks

### ✅ Phase 1: Service Layer Changes

- [ ] **Task 1.1:** Modify `AuthService.validateToken()` method
  - [ ] Remove `InvalidTokenException` throw when token not found in database (line 98)
  - [ ] Remove `InvalidTokenException` throw when session expired (line 101)
  - [ ] Return `ValidateResponse` with `valid: false` for invalid tokens
  - [ ] Keep returning `valid: true` with full data for valid tokens
  - **File:** `Auth/src/main/java/com/onlineshop/auth/service/AuthService.java`

### ✅ Phase 2: Controller Layer Changes

- [ ] **Task 2.1:** Create new exception for invalid token format
  - [ ] Create `InvalidTokenFormatException` class
  - [ ] Use this for format validation errors (empty header, no Bearer prefix)
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/InvalidTokenFormatException.java` (new)

- [ ] **Task 2.2:** Update `AuthController.extractToken()` method
  - [ ] Add check for empty Authorization header → throw `InvalidTokenFormatException`
  - [ ] Keep check for missing "Bearer: " prefix → throw `InvalidTokenFormatException`
  - [ ] Add check for empty token after "Bearer: " → throw `InvalidTokenFormatException`
  - **File:** `Auth/src/main/java/com/onlineshop/auth/controller/AuthController.java`

- [ ] **Task 2.3:** Update `AuthController.validate()` method
  - [ ] Remove `@NotBlank` validation on Authorization header
  - [ ] Add explicit check for missing header → throw custom exception for 400
  - **File:** `Auth/src/main/java/com/onlineshop/auth/controller/AuthController.java`

### ✅ Phase 3: Exception Handler Changes

- [ ] **Task 3.1:** Create exception for missing Authorization header
  - [ ] Create `MissingAuthorizationHeaderException` class for 400 Bad Request
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/MissingAuthorizationHeaderException.java` (new)

- [ ] **Task 3.2:** Add handler for `InvalidTokenFormatException`
  - [ ] Return 401 Unauthorized with error response
  - [ ] Use error type: `https://api.onlineshop.com/errors/invalid-token-format`
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java`

- [ ] **Task 3.3:** Add handler for `MissingAuthorizationHeaderException`
  - [ ] Return 400 Bad Request with error response
  - [ ] Use error type: `https://api.onlineshop.com/errors/missing-authorization-header`
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java`

- [ ] **Task 3.4:** Update `handleMissingRequestHeaderException()`
  - [ ] Change Authorization header handling to return 400 Bad Request (currently 401)
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java` (line 140)

- [ ] **Task 3.5:** Keep `handleInvalidTokenException()` as-is
  - [ ] This should no longer be triggered by validateToken() for expired tokens
  - [ ] May still be used by other future endpoints if needed
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java`

### ✅ Phase 4: Unit Tests Updates

- [ ] **Task 4.1:** Update `AuthServiceTest.validateToken_whenTokenNotFound_throwsInvalidTokenException()`
  - [ ] Rename to `validateToken_whenTokenNotFound_returnsInvalidResponse()`
  - [ ] Change assertion from expecting exception to expecting `valid: false` response
  - [ ] Assert other fields are null
  - **File:** `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java` (line 187-195)

- [ ] **Task 4.2:** Update `AuthServiceTest.validateToken_whenSessionExpired_throwsInvalidTokenException()`
  - [ ] Rename to `validateToken_whenSessionExpired_returnsInvalidResponse()`
  - [ ] Change assertion from expecting exception to expecting `valid: false` response
  - [ ] Assert other fields are null
  - **File:** `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java` (line 198-210)

- [ ] **Task 4.3:** Update `AuthServiceTest.validateToken_whenCurrentTimeBeforeCreatedAt_throwsInvalidTokenException()`
  - [ ] Rename to `validateToken_whenCurrentTimeBeforeCreatedAt_returnsInvalidResponse()`
  - [ ] Change assertion from expecting exception to expecting `valid: false` response
  - [ ] Assert other fields are null
  - **File:** `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java` (line 213-225)

### ✅ Phase 5: Integration Tests Updates

- [ ] **Task 5.1:** Update `ComponentIT.validate_withInvalidRandomToken_returnsUnauthorized()`
  - [ ] Rename to `validate_withInvalidRandomToken_returnsValidFalse()`
  - [ ] Change expectation from 401 to 200 OK
  - [ ] Assert response contains `valid: false`
  - [ ] Assert userId, username, createdAt, expiresAt are null
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (line 200-215)

- [ ] **Task 5.2:** Keep `ComponentIT.validate_withMissingBearerPrefix_returnsUnauthorized()` as-is
  - [ ] This should still return 401 for invalid format
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (line 186-198)

- [ ] **Task 5.3:** Add new test for expired token returning 200 with valid=false
  - [ ] Create `validate_withExpiredToken_returnsValidFalse()` test
  - [ ] Register user, login, manually expire the session in DB, then validate
  - [ ] Assert 200 OK with `valid: false`
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 5.4:** Update `EndpointsParametersIT.invalidValidateHeadersProvider()` data
  - [ ] Update "missing Authorization header" → expect 400 Bad Request
  - [ ] Keep "empty Authorization header" → expect 401 Unauthorized
  - [ ] Keep "invalid format" cases → expect 401 Unauthorized
  - [ ] Update "invalid token" → expect 200 OK (then check valid=false in test)
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/EndpointsParametersIT.java` (line 170-179)

- [ ] **Task 5.5:** Update `EndpointsParametersIT.validate_withInvalidAuthHeader_returnsUnauthorized()`
  - [ ] Rename to `validate_withInvalidAuthHeader_returnsExpectedStatus()`
  - [ ] Update to handle both error responses (400/401) and success response (200 with valid=false)
  - [ ] Add separate assertion logic for each status code
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/EndpointsParametersIT.java` (line 95-122)

### ✅ Phase 6: Add Comprehensive Integration Tests for New Cases

- [ ] **Task 6.1:** Add test for missing Authorization header returns 400
  - [ ] Create `validate_withMissingAuthorizationHeader_returnsBadRequest()` test
  - [ ] Call validate endpoint without Authorization header
  - [ ] Assert 400 Bad Request status
  - [ ] Assert error response with type `https://api.onlineshop.com/errors/missing-authorization-header`
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 6.2:** Add test for empty Authorization header returns 401
  - [ ] Create `validate_withEmptyAuthorizationHeader_returnsUnauthorized()` test
  - [ ] Call validate endpoint with empty string header
  - [ ] Assert 401 Unauthorized status
  - [ ] Assert error response with type `https://api.onlineshop.com/errors/invalid-token-format`
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 6.3:** Add test for blank token after "Bearer: " returns 401
  - [ ] Create `validate_withBlankTokenAfterBearer_returnsUnauthorized()` test
  - [ ] Call validate endpoint with "Bearer:    " (spaces only)
  - [ ] Assert 401 Unauthorized status
  - [ ] Assert error response with type `https://api.onlineshop.com/errors/invalid-token-format`
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 6.4:** Add test for token not in database returns 200 with valid=false
  - [ ] Create `validate_withNonExistentToken_returnsValidFalse()` test
  - [ ] Generate a valid-format token that doesn't exist in DB
  - [ ] Call validate endpoint with "Bearer: " + token
  - [ ] Assert 200 OK status
  - [ ] Assert response: `valid: false`, all other fields null
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 6.5:** Add test for future session (createdAt > now) returns 200 with valid=false
  - [ ] Create `validate_withFutureSession_returnsValidFalse()` test
  - [ ] Register user, login, manually update session createdAt to future time
  - [ ] Call validate endpoint
  - [ ] Assert 200 OK status
  - [ ] Assert response: `valid: false`, all other fields null
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

- [ ] **Task 6.6:** Add test verifying valid token still works correctly
  - [ ] Create `validate_withValidToken_returnsValidTrueWithUserData()` test
  - [ ] Register user, login, immediately validate
  - [ ] Assert 200 OK status
  - [ ] Assert response: `valid: true`, userId, username, createdAt, expiresAt all populated
  - **File:** `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java` (new test)

### ✅ Phase 7: Remove Redundant Code

- [ ] **Task 7.1:** Analyze `InvalidTokenException` usage
  - [ ] Search for all usages of `InvalidTokenException` in the codebase
  - [ ] Verify it's only used in `AuthController.extractToken()` (now replaced with `InvalidTokenFormatException`)
  - [ ] If no other usages exist, mark for potential removal (keep for now as it might be used in future)
  - **Files to check:** All `.java` files in Auth project

- [ ] **Task 7.2:** Remove unused imports from AuthService
  - [ ] Remove `InvalidTokenException` import if no longer used (line 7)
  - **File:** `Auth/src/main/java/com/onlineshop/auth/service/AuthService.java`

- [ ] **Task 7.3:** Remove unused imports from AuthController
  - [ ] Remove `InvalidTokenException` import if replaced by new exceptions (line 4)
  - **File:** `Auth/src/main/java/com/onlineshop/auth/controller/AuthController.java`

- [ ] **Task 7.4:** Remove unused imports from test files
  - [ ] Remove `InvalidTokenException` import from AuthServiceTest if no longer needed
  - **File:** `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java`

- [ ] **Task 7.5:** Clean up `@Validated` annotation if no longer needed
  - [ ] Check if `@Validated` on AuthController is still needed after removing `@NotBlank`
  - [ ] If no other validation annotations exist, remove `@Validated` and its import
  - **File:** `Auth/src/main/java/com/onlineshop/auth/controller/AuthController.java`

- [ ] **Task 7.6:** Review and update `handleConstraintViolationException()`
  - [ ] Check if the Authorization header validation logic is still needed
  - [ ] Remove Authorization-specific handling if `@NotBlank` validation is removed
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java` (line 168-206)

- [ ] **Task 7.7:** Consider deprecating `handleInvalidTokenException()`
  - [ ] Add `@Deprecated` annotation with comment explaining it's kept for backward compatibility
  - [ ] Add comment explaining when it can be removed (e.g., "Remove in next major version")
  - **File:** `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java` (line 118-131)

### ✅ Phase 8: Build and Verify

- [ ] **Task 8.1:** Build the project
  - [ ] Run `./mvnw clean compile` from Auth directory
  - [ ] Fix any compilation errors

- [ ] **Task 8.2:** Run unit tests
  - [ ] Run `./mvnw test` from Auth directory
  - [ ] Ensure all unit tests pass

- [ ] **Task 8.3:** Run integration tests
  - [ ] Run `./mvnw verify` from Auth directory
  - [ ] Ensure all integration tests pass

- [ ] **Task 8.4:** Verify all new test cases pass
  - [ ] Confirm all 6 new integration tests from Phase 6 pass
  - [ ] Confirm all updated tests from Phase 5 pass
  - [ ] Check test coverage remains above threshold (>65%)

---

## Technical Issues & Solutions

### Issue 1: ValidateResponse fields nullability
**Problem:** Fields like `userId`, `username`, etc. need to be nullable for `valid: false` responses
**Solution:** ✅ Fields are already nullable (using Long, String, Instant) with Lombok `@Builder`

### Issue 2: Distinguishing between missing and empty Authorization header
**Problem:** Need different status codes for missing (400) vs empty (401)
**Solution:** Use custom exceptions:
- `MissingAuthorizationHeaderException` → 400 Bad Request
- `InvalidTokenFormatException` → 401 Unauthorized

### Issue 3: Backwards compatibility with API consumers
**Problem:** Changing from 401 to 200 for invalid tokens might break clients
**Solution:** ✅ This is an intentional breaking change as per requirements. Document in API changelog.

---

## Files to Modify

### New Files
1. `Auth/src/main/java/com/onlineshop/auth/exception/InvalidTokenFormatException.java`
2. `Auth/src/main/java/com/onlineshop/auth/exception/MissingAuthorizationHeaderException.java`

### Modified Files
1. `Auth/src/main/java/com/onlineshop/auth/service/AuthService.java`
2. `Auth/src/main/java/com/onlineshop/auth/controller/AuthController.java`
3. `Auth/src/main/java/com/onlineshop/auth/exception/GlobalExceptionHandler.java`
4. `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java`
5. `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/ComponentIT.java`
6. `Auth/src/integrationTest/java/com/onlineshop/auth/component/integration/EndpointsParametersIT.java`

---

## Success Criteria

✅ All compilation succeeds
✅ All unit tests pass (including 3 updated tests)
✅ All integration tests pass (including 6 new comprehensive tests)
✅ No redundant code or unused imports remain
✅ Missing Authorization header → 400 Bad Request
✅ Empty/invalid format Authorization header → 401 Unauthorized
✅ Invalid or expired token with correct format → 200 OK with `valid: false`
✅ Valid token → 200 OK with full user data
✅ Test coverage remains above 65%

### New Integration Tests Coverage
- Missing Authorization header → 400
- Empty Authorization header → 401
- Blank token after "Bearer: " → 401
- Non-existent token in DB → 200 with valid=false
- Future session (invalid time) → 200 with valid=false
- Valid token → 200 with valid=true and full data

---

## Notes

- **Scope:** Changes are confined to Auth project only (as per requirement)
- **Testing Strategy:**
  - 3 existing unit tests updated to expect new behavior
  - 5 existing integration tests updated for new status codes
  - 6 new comprehensive integration tests cover all edge cases
  - Total test coverage: All possible scenarios for missing/empty/invalid/expired/valid tokens
- **Code Cleanup:** Phase 7 ensures no redundant code, unused imports, or obsolete handlers remain
- **Backward Compatibility:** This is an intentional breaking change to the validate endpoint API
- **Documentation:** No external documentation updates needed (internal change)
- **Success Indicator:** After all tests pass in Phase 8, implementation is complete

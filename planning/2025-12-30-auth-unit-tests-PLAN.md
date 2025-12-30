# Plan: Unit Tests for Auth Service

## Overview

Create comprehensive unit tests for `AuthService` targeting >90% coverage as per project standards. Includes refactoring for better testability.

## Files to Create/Modify

| Action | File |
|--------|------|
| Modify | `Auth/pom.xml` (add JaCoCo plugin for coverage) |
| Modify | `Auth/src/main/java/com/onlineshop/auth/entity/Session.java` (refactor isExpired) |
| Modify | `Auth/src/main/java/com/onlineshop/auth/service/AuthService.java` (inject SecureRandom) |
| Create | `Auth/src/main/java/com/onlineshop/auth/config/AuthConfig.java` (SecureRandom bean) |
| Create | `Auth/src/test/java/com/onlineshop/auth/service/AuthServiceTest.java` |

## Step 0: Refactorings for Testability

### 0.1 Refactor Session.isExpired()
Change from:
```java
public boolean isExpired() {
    return Instant.now().isAfter(expiresAt);
}
```
To:
```java
public boolean isExpired(Instant now) {
    return now.isAfter(expiresAt);
}
```

### 0.2 Inject SecureRandom into AuthService
- Add `SecureRandom` as constructor parameter
- Create `AuthConfig.java` with `@Bean SecureRandom secureRandom()`
- Update `generateToken()` to use injected instance

## Step 1: Add JaCoCo Plugin to pom.xml

Add JaCoCo maven plugin with exclusions per TESTING_STRATEGY.md:
- `**/config/**`
- `**/*Application.*`
- `**/dto/**`
- `**/entity/**`

## Step 2: Create AuthServiceTest.java

### Test Class Structure

```java
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {
    @Mock private UserRepository userRepository;
    @Mock private SessionRepository sessionRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private SecureRandom secureRandom;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, sessionRepository,
                                      passwordEncoder, secureRandom, 3600L);
    }
}
```

### Test Cases (15 tests)

#### register() - 4 tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `register_whenUsernameAvailable_createsUserAndReturnsResponse` | New username | Returns RegisterResponse with user data |
| `register_whenUsernameExists_throwsUserAlreadyExistsException` | Duplicate username | Throws UserAlreadyExistsException |
| `register_whenUsernameExistsWithDifferentCase_throwsUserAlreadyExistsException` | "ivan" exists, register "Ivan" | Throws UserAlreadyExistsException (TDD - test first) |
| `register_savesUserWithEncodedPassword` | Any registration | User saved with encoded password (verify via ArgumentCaptor) |

#### login() - 5 tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `login_whenCredentialsValid_returnsTokenAndSession` | Valid user+password | Returns LoginResponse with token, creates session |
| `login_whenUserNotFound_throwsInvalidUsernameOrPasswordException` | Unknown username | Throws InvalidUsernameOrPasswordException |
| `login_whenPasswordInvalid_throwsInvalidUsernameOrPasswordException` | Wrong password | Throws InvalidUsernameOrPasswordException |
| `login_generatesUniqueTokensForEachCall` | Multiple logins | Different tokens generated |
| `login_setsCorrectSessionExpiration` | Valid login | Session expiresAt = now + sessionExpirationSeconds |

#### validateToken() - 4 tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `validateToken_whenTokenValidAndNotExpired_returnsValidateResponse` | Valid, non-expired session | Returns ValidateResponse with valid=true |
| `validateToken_whenTokenNotFound_throwsInvalidTokenException` | Unknown token | Throws InvalidTokenException |
| `validateToken_whenSessionExpired_throwsInvalidTokenException` | Expired session | Throws InvalidTokenException |
| `validateToken_hashesTokenBeforeLookup` | Any token | sessionRepository.findByTokenHash() called with SHA-256 hash |

#### Token Generation - 2 tests

| Test Name | Scenario | Expected |
|-----------|----------|----------|
| `login_generatedTokenHasCorrectFormat` | Any login | Token is 64 hex chars (32 bytes) |
| `login_tokenHashIsDeterministic` | Same token hashed twice | Produces identical hash |

## Step 3: Testing Notes

### Private methods (generateToken, hashToken)
Test indirectly through public methods. Verify token format (64 hex chars) and hash consistency.

### SecureRandom (now injected)
Mock `secureRandom.nextBytes()` to return deterministic values for predictable token generation.

### Session.isExpired(Instant now) (refactored)
Pass controlled `Instant` values directly - no workarounds needed.

### Verifying correct hash is used for lookup
Use ArgumentCaptor to capture the tokenHash passed to repository. Verify it matches SHA-256 hash of original token.

## Step 4: Test Utilities

Create helper methods in test class:
```java
private User createUser(Long id, String username, String passwordHash);
private Session createSession(User user, String tokenHash, Instant expiresAt);
private RegisterRequest createRegisterRequest(String username, String password);
private LoginRequest createLoginRequest(String username, String password);
```

## Execution Order

1. Add JaCoCo plugin to `Auth/pom.xml`
2. Create test directory structure: `Auth/src/test/java/com/onlineshop/auth/service/`
3. Create `AuthServiceTest.java` with all 14 test cases
4. Run tests: `./mvnw.cmd clean test`
5. Generate coverage report: `./mvnw.cmd jacoco:report`

## Expected Coverage

| Class | Target | Notes |
|-------|--------|-------|
| AuthService | 100% | All public methods tested |
| Exceptions | Excluded | Trivial classes |
| DTOs | Excluded | No logic |
| Entities | Excluded | Framework-managed |
| Config | Excluded | Spring configuration |

## Dependencies (already in pom.xml)

- `spring-boot-starter-test` (includes JUnit 5, Mockito, AssertJ)

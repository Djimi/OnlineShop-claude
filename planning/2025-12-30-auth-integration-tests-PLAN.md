# Plan: Integration Tests for Auth Service

## Implementation Status

| Step | Status | Notes |
|------|--------|-------|
| Add Testcontainers dependencies | DONE | Also added `spring-boot-test-autoconfigure` and `jackson-databind` for MockMvc |
| Create application-integration-test.yml | DONE | |
| Create testcontainers.properties | DONE | Required for Docker Desktop on Windows |
| Create BaseIntegrationTest | DONE | |
| Create TestDataFactory | DONE | |
| Create UserRepositoryIntegrationTest | DONE | 7 tests |
| Create SessionRepositoryIntegrationTest | DONE | 11 tests |
| Create AuthControllerIntegrationTest | DONE | 11 tests |
| Run & verify tests | BLOCKED | Requires Docker Desktop TCP configuration |

---

## Known Issues

### 1. Docker Desktop TCP Configuration Required (Windows)

**Problem**: Testcontainers cannot connect to Docker via Windows named pipes on this system. The Java Docker client receives empty responses from the Docker API.

**Solution**: Enable TCP in Docker Desktop:
1. Open Docker Desktop → Settings → General
2. Check **"Expose daemon on tcp://localhost:2375 without TLS"**
3. Click "Apply & Restart"

**File created**: `Auth/src/test/resources/testcontainers.properties`
```properties
docker.host=tcp://localhost:2375
testcontainers.reuse.enable=true
```

### 2. Pre-existing Failing Unit Test

**Test**: `AuthServiceTest.register_whenUsernameExistsWithDifferentCase_throwsUserAlreadyExistsException`

**Reason**: This is a TDD "red" test waiting for case-insensitive username checking to be implemented. Not related to integration tests.

---

## Overview

Implement integration tests for Auth service using Testcontainers with PostgreSQL. Tests verify repository methods with real database and controller endpoints with actual HTTP requests.

## Files Created/Modified

| Status | Action | File |
|--------|--------|------|
| DONE | Modify | `Auth/pom.xml` - Added Testcontainers + MockMvc dependencies |
| DONE | Create | `Auth/src/test/resources/application-integration-test.yml` |
| DONE | Create | `Auth/src/test/resources/testcontainers.properties` |
| DONE | Create | `Auth/src/test/java/.../integration/BaseIntegrationTest.java` |
| DONE | Create | `Auth/src/test/java/.../integration/repository/UserRepositoryIntegrationTest.java` |
| DONE | Create | `Auth/src/test/java/.../integration/repository/SessionRepositoryIntegrationTest.java` |
| DONE | Create | `Auth/src/test/java/.../integration/controller/AuthControllerIntegrationTest.java` |
| DONE | Create | `Auth/src/test/java/.../testutil/TestDataFactory.java` |

---

## Step 1: Add Dependencies to pom.xml

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.testcontainers</groupId>
            <artifactId>testcontainers-bom</artifactId>
            <version>1.20.4</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<!-- Add to dependencies section -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
```

---

## Step 2: Create Test Resources

### application-integration-test.yml
```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate
    show-sql: true

session:
  expiration: 3600
```

Schema initialization uses existing file via `@Sql("file:init-db/01-schema.sql")` in BaseIntegrationTest.

---

## Step 3: Create BaseIntegrationTest

Shared base class with:
- `@SpringBootTest(webEnvironment = RANDOM_PORT)`
- `@Testcontainers` with static PostgreSQL container
- `@DynamicPropertySource` to inject container connection
- `@ActiveProfiles("integration-test")`
- `@Sql("file:init-db/01-schema.sql")` to reuse existing schema

---

## Step 4: Repository Integration Tests

### UserRepositoryIntegrationTest (7 tests)

| Test | Scenario |
|------|----------|
| `findByUsername_whenUserExists_returnsUser` | Found user |
| `findByUsername_whenUserNotFound_returnsEmpty` | Not found |
| `findByUsername_isCaseSensitive` | Case sensitivity check |
| `existsByUsername_whenUserExists_returnsTrue` | Exists |
| `existsByUsername_whenUserNotFound_returnsFalse` | Not exists |
| `save_whenValidUser_persistsAndGeneratesId` | Persistence |
| `save_whenDuplicateUsername_throwsDataIntegrityViolation` | Unique constraint |

### SessionRepositoryIntegrationTest (11 tests)

| Test | Scenario |
|------|----------|
| `findByTokenHash_whenSessionExists_returnsSession` | Found session |
| `findByTokenHash_whenSessionNotFound_returnsEmpty` | Not found |
| `findByTokenHash_loadsUserRelationship` | Lazy load works |
| `deleteExpiredSessions_whenExpiredSessionsExist_deletesOnlyExpired` | Selective delete |
| `deleteExpiredSessions_whenNoExpiredSessions_deletesNothing` | No-op case |
| `deleteExpiredSessions_withBoundaryTime_handlesCorrectly` | Boundary condition |
| `deleteByUserId_whenUserHasSessions_deletesAllUserSessions` | User sessions deleted |
| `deleteByUserId_whenUserHasNoSessions_doesNothing` | No-op case |
| `save_whenValidSession_persistsAndGeneratesId` | Persistence |
| `deleteUser_cascadesDeleteToSessions` | CASCADE DELETE works |
| `save_whenDuplicateTokenHash_throwsDataIntegrityViolation` | Unique constraint |

---

## Step 5: Controller Integration Tests

### AuthControllerIntegrationTest (11 tests)

| Test | Endpoint | Scenario |
|------|----------|----------|
| `register_whenValidRequest_returns201WithUserDetails` | POST /register | Success |
| `register_whenUsernameExists_returns409Conflict` | POST /register | Duplicate |
| `register_passwordIsHashedInDatabase` | POST /register | Hash verification |
| `login_whenValidCredentials_returns200WithToken` | POST /login | Success |
| `login_whenUserNotFound_returns401Unauthorized` | POST /login | User not found |
| `login_whenWrongPassword_returns401Unauthorized` | POST /login | Wrong password |
| `login_createsSessionInDatabase` | POST /login | Session persistence |
| `validate_whenValidToken_returns200WithUserInfo` | GET /validate | Valid token |
| `validate_whenTokenNotFound_returns401Unauthorized` | GET /validate | Invalid token |
| `validate_whenTokenExpired_returns401Unauthorized` | GET /validate | Expired token |
| `fullAuthenticationFlow_registerLoginValidate_succeeds` | All | E2E flow |

---

## Step 6: Test Data Factory

Utility class with:
- `createUser(username, passwordHash)` - User builder
- `createSession(tokenHash, user, expiresAt)` - Session builder
- `createRegisterRequest/LoginRequest` - DTO builders
- `hashToken(token)` - SHA-256 hashing utility

---

## Test Summary

| Category | Tests |
|----------|-------|
| Existing Unit Tests | 15 |
| UserRepository Integration | 7 |
| SessionRepository Integration | 11 |
| AuthController Integration | 11 |
| **Total** | **44** |

---

## Implementation Order

1. Add Testcontainers dependencies to pom.xml
2. Create test resources (application-test.yml, init-schema.sql)
3. Create BaseIntegrationTest
4. Create TestDataFactory
5. Create UserRepositoryIntegrationTest - run & verify
6. Create SessionRepositoryIntegrationTest - run & verify
7. Create AuthControllerIntegrationTest - run & verify
8. Run full test suite

---

## Key Design Decisions

- **Static container**: Shared across all tests for speed
- **`@BeforeEach` cleanup**: Delete all data before each test (sessions first, then users)
- **No `@Transactional` on test class**: Tests see real commit behavior, `save()` commits immediately
- **Reuse existing schema**: Reference `init-db/01-schema.sql` instead of duplicating
- **PostgreSQL 18-alpine**: Matches production docker-compose

---

## How to Run

### Prerequisites
1. Docker Desktop running with TCP enabled (see Known Issues above)

### Commands

```bash
# Run only integration tests
./mvnw.cmd test -Dtest="*IntegrationTest"

# Run all tests (includes unit tests)
./mvnw.cmd test

# Run specific test class
./mvnw.cmd test -Dtest="UserRepositoryIntegrationTest"
```

### Expected Results

Once Docker TCP is configured:
- 29 new integration tests should pass (7 + 11 + 11)
- 14 existing unit tests should pass
- 1 existing unit test fails (TDD red test for case-insensitive usernames)

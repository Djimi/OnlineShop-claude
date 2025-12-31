# Plan: Integration Tests for Auth Service

## Implementation Status

| Step | Status | Notes |
|------|--------|-------|
| Add Testcontainers dependencies | DONE | Also added `spring-boot-test-autoconfigure` and `jackson-databind` for MockMvc |
| Configure JaCoCo for integration tests | DONE | Separate reports for unit/integration + merged report |
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

## JaCoCo Code Coverage for Integration Tests

### Background: Maven Test Plugins

Maven has **two different plugins** for running tests:

| Plugin | Purpose | Default Test Naming | Maven Phase |
|--------|---------|---------------------|-------------|
| **maven-surefire-plugin** | Unit tests | `*Test.java`, `Test*.java` | `test` |
| **maven-failsafe-plugin** | Integration tests | `*IT.java`, `*IntegrationTest.java` | `integration-test` |

**Why separate plugins?**
- Unit tests are fast and run in isolation (no external dependencies)
- Integration tests are slower and may need external resources (databases, containers)
- Failsafe is designed to **not fail the build immediately** if a test fails - it waits until the `verify` phase so cleanup can happen

### How JaCoCo Works

JaCoCo (Java Code Coverage) works by **instrumenting** your compiled bytecode. It:
1. Attaches a Java agent to the JVM running tests
2. Records which lines of code are executed during test runs
3. Writes execution data to `.exec` files
4. Generates HTML/XML reports from the `.exec` files

### Configuration Added to pom.xml

We added the **maven-failsafe-plugin** and enhanced JaCoCo with these executions:

```xml
<!-- 1. Failsafe plugin - runs *IntegrationTest.java files -->
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-failsafe-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>integration-test</goal>  <!-- Runs integration tests -->
                <goal>verify</goal>            <!-- Fails build if tests failed -->
            </goals>
        </execution>
    </executions>
</plugin>
```

```xml
<!-- 2. JaCoCo executions for coverage -->
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <executions>
        <!-- Unit test coverage -->
        <execution>
            <id>prepare-agent</id>
            <goals><goal>prepare-agent</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/jacoco-ut.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-ut.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-ut</outputDirectory>
            </configuration>
        </execution>

        <!-- Integration test coverage -->
        <execution>
            <id>prepare-agent-integration</id>
            <goals><goal>prepare-agent-integration</goal></goals>
            <configuration>
                <destFile>${project.build.directory}/jacoco-it.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>report-integration</id>
            <phase>post-integration-test</phase>
            <goals><goal>report-integration</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-it.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-it</outputDirectory>
            </configuration>
        </execution>

        <!-- Merged report (unit + integration combined) -->
        <execution>
            <id>merge-results</id>
            <phase>verify</phase>
            <goals><goal>merge</goal></goals>
            <configuration>
                <fileSets>
                    <fileSet>
                        <directory>${project.build.directory}</directory>
                        <includes>
                            <include>jacoco-ut.exec</include>
                            <include>jacoco-it.exec</include>
                        </includes>
                    </fileSet>
                </fileSets>
                <destFile>${project.build.directory}/jacoco-merged.exec</destFile>
            </configuration>
        </execution>
        <execution>
            <id>report-merged</id>
            <phase>verify</phase>
            <goals><goal>report</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
                <outputDirectory>${project.reporting.outputDirectory}/jacoco-merged</outputDirectory>
            </configuration>
        </execution>

        <!-- Coverage check uses merged data -->
        <execution>
            <id>check</id>
            <phase>verify</phase>
            <goals><goal>check</goal></goals>
            <configuration>
                <dataFile>${project.build.directory}/jacoco-merged.exec</dataFile>
                <rules>
                    <rule>
                        <element>BUNDLE</element>
                        <limits>
                            <limit>
                                <counter>LINE</counter>
                                <value>COVEREDRATIO</value>
                                <minimum>0.90</minimum>
                            </limit>
                        </limits>
                    </rule>
                </rules>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### Maven Lifecycle Phases (Execution Order)

When you run `mvn verify`, Maven executes phases in this order:

```
compile → test-compile → test → package → pre-integration-test →
integration-test → post-integration-test → verify → install
```

| Phase | What Happens | JaCoCo Action |
|-------|--------------|---------------|
| `test-compile` | Compile test classes | - |
| `test` | **Surefire runs unit tests** (`*Test.java`) | `prepare-agent` instruments code, `report` generates unit report |
| `pre-integration-test` | Setup before integration tests | `prepare-agent-integration` instruments code |
| `integration-test` | **Failsafe runs integration tests** (`*IntegrationTest.java`) | Tests write to `jacoco-it.exec` |
| `post-integration-test` | Cleanup after integration tests | `report-integration` generates integration report |
| `verify` | Verify build succeeded | `merge` combines exec files, `report-merged` generates combined report, `check` enforces 90% coverage |

### Generated Files

After running `mvn verify`, you'll find:

```
Auth/target/
├── jacoco-ut.exec              # Unit test execution data (binary)
├── jacoco-it.exec              # Integration test execution data (binary)
├── jacoco-merged.exec          # Combined execution data (binary)
└── site/
    ├── jacoco-ut/              # Unit test HTML report
    │   └── index.html
    ├── jacoco-it/              # Integration test HTML report
    │   └── index.html
    └── jacoco-merged/          # Combined HTML report
        └── index.html
```

### Understanding the HTML Reports

Open any `index.html` in a browser. The report shows:

- **Element**: Package/Class/Method being measured
- **Missed Instructions**: Bytecode instructions not executed (red)
- **Cov.**: Coverage percentage
- **Missed Branches**: Conditional branches not taken
- **Missed Lines**: Lines of code not executed

Color coding in source view:
- 🟢 **Green**: Fully covered
- 🟡 **Yellow**: Partially covered (some branches not taken)
- 🔴 **Red**: Not covered at all

### Why Three Separate Reports?

| Report | Use Case |
|--------|----------|
| `jacoco-ut` | See what unit tests cover (fast feedback) |
| `jacoco-it` | See what integration tests cover (real DB/HTTP behavior) |
| `jacoco-merged` | **Total coverage** - what the 90% check uses |

This helps identify:
- Code only tested by unit tests (mocks may miss real bugs)
- Code only tested by integration tests (may need unit test edge cases)
- Code not tested at all

### Exclusions

These packages are excluded from coverage (configured in pom.xml):
- `**/config/**` - Spring configuration classes
- `**/*Application.*` - Main application class
- `**/dto/**` - Data transfer objects (simple POJOs)
- `**/entity/**` - JPA entities (simple POJOs)

---

## How to Run

### Prerequisites
1. Docker Desktop running with TCP enabled (see Known Issues above)

### Commands

```bash
# Run only unit tests (with unit test coverage report)
./mvnw.cmd test
# Report: target/site/jacoco-ut/index.html

# Run unit + integration tests with FULL coverage
./mvnw.cmd verify
# Reports: target/site/jacoco-ut/index.html (unit only)
#          target/site/jacoco-it/index.html (integration only)
#          target/site/jacoco-merged/index.html (combined - used for 90% check)

# Run specific integration test class
./mvnw.cmd verify -Dit.test="UserRepositoryIntegrationTest"

# Skip unit tests, run only integration tests
./mvnw.cmd verify -DskipTests -Dit.test="*IntegrationTest"

# Skip coverage check (useful during development)
./mvnw.cmd verify -Djacoco.skip=true
```

**Important:** Use `mvn verify` (not `mvn test`) to run integration tests. The `test` phase only runs Surefire (unit tests). The `verify` phase runs both Surefire and Failsafe (integration tests).

### Expected Results

Once Docker TCP is configured:
- 29 new integration tests should pass (7 + 11 + 11)
- 14 existing unit tests should pass
- 1 existing unit test fails (TDD red test for case-insensitive usernames)

# Integration Test Troubleshooting Guide

This document captures all issues encountered while setting up integration tests with Testcontainers and PostgreSQL in Spring Boot 4.x, along with their solutions.

## Environment
- Spring Boot 4.0.1
- Testcontainers 2.0.3
- PostgreSQL 18-alpine
- Java 25
- Hibernate with `ddl-auto: validate`

---

## Issue 1: Schema validation - missing table [sessions]

### Symptom
```
Schema-validation: missing table [sessions]
Schema-validation: missing table [users]
```

### Root Cause
Using `@Sql` annotation with `executionPhase = BEFORE_TEST_CLASS` to run the schema script. The problem is timing:
- Hibernate's `ddl-auto: validate` runs **DURING** Spring context creation
- `@Sql` with `BEFORE_TEST_CLASS` runs **AFTER** Spring context is created

This creates a chicken-and-egg problem where validation fails before the script can execute.

### Solution
Use Testcontainers' `withInitScript()` method instead of `@Sql`:

```java
@Container
@ServiceConnection
static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine")
        .withDatabaseName("auth_test")
        .withUsername("test")
        .withPassword("test")
        .withInitScript("init-db/01-schema.sql");  // Runs before Spring context starts
```

---

## Issue 2: Init script not found on classpath

### Symptom
```
Could not read init script file: init-db/01-schema.sql
```

### Root Cause
The `init-db` directory wasn't included in test resources, so it wasn't available on the classpath during test execution.

### Solution
Add the `init-db` directory as a test resource in `pom.xml` using build-helper-maven-plugin:

```xml
<plugin>
    <groupId>org.codehaus.mojo</groupId>
    <artifactId>build-helper-maven-plugin</artifactId>
    <version>3.5.0</version>
    <executions>
        <execution>
            <id>add-test-resource</id>
            <phase>generate-test-resources</phase>
            <goals>
                <goal>add-test-resource</goal>
            </goals>
            <configuration>
                <resources>
                    <resource>
                        <directory>src/integrationTest/java/resources</directory>
                    </resource>
                    <resource>
                        <directory>init-db</directory>
                        <targetPath>init-db</targetPath>
                    </resource>
                </resources>
            </configuration>
        </execution>
    </executions>
</plugin>
```

---

## Issue 3: MockMvc bean not found

### Symptom
```
No qualifying bean of type 'org.springframework.test.web.servlet.MockMvc' available
```

### Root Cause
MockMvc isn't auto-configured by default in `@SpringBootTest`. Need to either use `@AutoConfigureMockMvc` or configure it manually.

### Solution Attempt 1 (Failed)
Tried to use `@AutoConfigureMockMvc` annotation but got compilation error (see Issue 4).

### Solution (Final)
Manually configure MockMvc using `MockMvcBuilders`:

```java
@Autowired
private WebApplicationContext webApplicationContext;

private MockMvc mockMvc;

@BeforeEach
void setUpMockMvc() {
    mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
}
```

---

## Issue 4: @AutoConfigureMockMvc package not found

### Symptom
```
package org.springframework.boot.test.autoconfigure.web.servlet does not exist
```

### Root Cause
Spring Boot 4.x restructured test auto-configuration packages. The `@AutoConfigureMockMvc` annotation may have moved or requires additional dependencies.

### Solution
Instead of debugging package locations, use manual MockMvc configuration (see Issue 3 solution). This is more explicit and works reliably across Spring Boot versions.

---

## Issue 5: Connection refused between test classes

### Symptom
```
Connection refused: localhost:XXXXX
```
Occurring when running multiple test classes sequentially.

### Root Cause
Spring Test Context Framework caches application contexts between test classes for performance. When a Testcontainer stops after one test class completes, the cached context still holds a reference to the old (now stopped) container's port.

### Solution
Add `@DirtiesContext` to force context recreation after each test class:

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {
    // ...
}
```

**Note:** This adds overhead as the context is recreated for each test class. For large test suites, consider using a single shared container with `@Testcontainers(disabledWithoutDocker = true)` and a singleton container pattern.

---

## Issue 6: ObjectMapper bean not available

### Symptom
```
No qualifying bean of type 'com.fasterxml.jackson.databind.ObjectMapper' available
```

### Root Cause
Spring Boot auto-configures `ObjectMapper` when using Spring MVC with `@RestController` and Jackson on classpath. However, this application's configuration doesn't trigger Jackson auto-configuration to expose the bean.

### Solution
Create ObjectMapper manually in the test class:

```java
private final ObjectMapper objectMapper = new ObjectMapper();
```

---

## Issue 7: Java 8 date/time type not supported

### Symptom
```
Java 8 date/time type `java.time.Instant` not supported by default:
add Module "com.fasterxml.jackson.datatype:jackson-datatype-jsr310" to enable handling
```

### Root Cause
When creating `ObjectMapper` manually (not using Spring's auto-configured one), the `JavaTimeModule` isn't registered by default. This module is required to serialize/deserialize Java 8 date/time types like `Instant`, `LocalDateTime`, etc.

### Solution

1. Add dependency to `pom.xml`:
```xml
<dependency>
    <groupId>com.fasterxml.jackson.datatype</groupId>
    <artifactId>jackson-datatype-jsr310</artifactId>
</dependency>
```

2. Register the module when creating ObjectMapper:
```java
private final ObjectMapper objectMapper = new ObjectMapper()
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
```

**Note:** If using Spring Boot's auto-configured `ObjectMapper` bean (via `@Autowired`), this module is registered automatically. Manual registration is only needed when creating ObjectMapper instances manually.

---

## Issue 8: LazyInitializationException

### Symptom
```
org.hibernate.LazyInitializationException: Could not initialize proxy [com.onlineshop.auth.entity.User#7] - no session
```

### Root Cause
The `Session` entity has a lazy-loaded `User` reference. When accessing `session.getUser().getUsername()` outside of a transaction/Hibernate session, the proxy cannot be initialized.

```java
// This triggers lazy loading and fails:
assertThat(session.get().getUser().getUsername()).isEqualTo("sessionuser");
```

### Solution Options

**Option 1: Compare IDs instead (Recommended for this case)**
Entity IDs are already loaded in the proxy and don't require lazy initialization:
```java
assertThat(session.get().getUser().getId()).isEqualTo(user.getId());
```

**Option 2: Add @Transactional to test method**
Keeps Hibernate session open for the duration of the test:
```java
@Test
@Transactional
void login_createsSessionInDatabase() throws Exception {
    // ...
}
```

**Option 3: Create eager-fetch repository method**
```java
@Query("SELECT s FROM Session s JOIN FETCH s.user WHERE s.tokenHash = :tokenHash")
Optional<Session> findByTokenHashWithUser(@Param("tokenHash") String tokenHash);
```

We chose Option 1 as it's the simplest fix and doesn't add transaction overhead or require repository changes.

---

## Final Working Configuration

### BaseIntegrationTest.java
```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@ActiveProfiles("integration-test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public abstract class BaseIntegrationTest {

    @Container
    @ServiceConnection
    @SuppressWarnings("resource")
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:18-alpine")
            .withDatabaseName("auth_test")
            .withUsername("test")
            .withPassword("test")
            .withInitScript("init-db/01-schema.sql");

    @Autowired
    protected UserRepository userRepository;

    @Autowired
    protected SessionRepository sessionRepository;

    @BeforeEach
    void cleanDatabase() {
        sessionRepository.deleteAll();
        userRepository.deleteAll();
    }
}
```

### AuthControllerIntegrationTest.java (key parts)
```java
class AuthControllerIntegrationTest extends BaseIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    @BeforeEach
    void setUpMockMvc() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    // ... tests
}
```

---

## Key Takeaways

1. **Testcontainers init scripts run before Spring context** - Use `withInitScript()` for schema setup with `ddl-auto: validate`

2. **Spring Boot 4.x has package changes** - Manual configuration may be more reliable than auto-configuration annotations

3. **Context caching can cause stale connections** - Use `@DirtiesContext` when containers are per-test-class

4. **Manual ObjectMapper needs manual module registration** - `JavaTimeModule` isn't auto-registered outside Spring's auto-configuration

5. **Lazy loading requires active session** - Compare IDs or use `@Transactional` when accessing lazy-loaded relationships in tests

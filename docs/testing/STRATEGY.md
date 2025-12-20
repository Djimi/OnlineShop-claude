# Testing Strategy

## Testing Pyramid

```
                    ▲
                   /  \           E2E Tests
                  /    \          (Few, Slow, High Confidence)
                 /──────\
                /        \        Integration Tests
               /          \       (Some, Medium Speed)
              /────────────\
             /              \     Unit Tests
            /                \    (Many, Fast, Focused)
           ────────────────────
```

## Test Types & Coverage Requirements

| Test Type | Coverage Target | Measured By | Scope |
|-----------|-----------------|-------------|-------|
| Unit | >90% | JaCoCo | Single class/function |
| Integration | Measured (no target) | JaCoCo | Multiple components, real DB |
| Contract | N/A | Pact/Spring Cloud Contract | API contracts |
| E2E | Scenario coverage | Checklist | Full system |

## Unit Tests

### Purpose
Test individual classes/functions in isolation.

### Tools
- **JUnit 5** - Test framework
- **Mockito** - Mocking dependencies
- **AssertJ** - Fluent assertions

### What to Test
- Business logic
- Validation rules
- Edge cases and corner cases
- Error handling

### What NOT to Test
- Simple getters/setters
- Framework code (Spring, JPA)
- Third-party libraries

### Example
```java
@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void findById_whenItemExists_returnsItem() {
        // Given
        var item = new Item(1L, "Test Item", BigDecimal.TEN);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item));

        // When
        var result = itemService.findById(1L);

        // Then
        assertThat(result.name()).isEqualTo("Test Item");
    }

    @Test
    void findById_whenItemNotFound_throwsException() {
        // Given
        when(itemRepository.findById(1L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> itemService.findById(1L))
            .isInstanceOf(ItemNotFoundException.class)
            .hasMessageContaining("1");
    }
}
```

### Running Unit Tests
```bash
# Run all unit tests
./mvnw.cmd test

# Run specific test class
./mvnw.cmd test -Dtest=ItemServiceTest

# Run with coverage
./mvnw.cmd test jacoco:report
```

## Integration Tests

### Purpose
Test multiple components working together with real dependencies.

### Tools
- **Spring Test** - ApplicationContext loading
- **Testcontainers** - Real database in Docker
- **@DataJpaTest** - JPA layer tests
- **@WebMvcTest** - Controller layer tests
- **@SpringBootTest** - Full application tests

### What to Test
- Repository queries actually work
- Controllers handle requests correctly
- Service layers integrate properly
- Database constraints work
- Corner cases - all non-happy paths, failures, cases with exceptions thrown, etc

### Example - Repository Test
```java
@DataJpaTest
@Testcontainers
class ItemRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    private ItemRepository itemRepository;

    @Test
    void save_persistsItemToDatabase() {
        // Given
        var item = new Item(null, "Test Item", BigDecimal.TEN);

        // When
        var saved = itemRepository.save(item);

        // Then
        assertThat(saved.getId()).isNotNull();

        var retrieved = itemRepository.findById(saved.getId());
        assertThat(retrieved).isPresent();
        assertThat(retrieved.get().getName()).isEqualTo("Test Item");
    }
}
```

### Example - Controller Test
```java
@WebMvcTest(ItemController.class)
class ItemControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ItemService itemService;

    @Test
    void getItem_whenExists_returns200() throws Exception {
        // Given
        var item = new ItemResponse(1L, "Test Item", BigDecimal.TEN);
        when(itemService.findById(1L)).thenReturn(item);

        // When/Then
        mockMvc.perform(get("/api/v1/items/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Test Item"));
    }

    @Test
    void getItem_whenNotFound_returns404() throws Exception {
        // Given
        when(itemService.findById(1L))
            .thenThrow(new ItemNotFoundException(1L));

        // When/Then
        mockMvc.perform(get("/api/v1/items/1"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.type").value(containsString("item-not-found")));
    }
}
```

### Running Integration Tests
```bash
# Run all tests (unit + integration)
./mvnw.cmd test

# Run only integration tests (by naming convention)
./mvnw.cmd test -Dtest="*IntegrationTest"
```

## Contract Tests

### Purpose
Ensure API contracts between services don't break.

### Tools (Future)
- **Spring Cloud Contract** - Consumer-driven contracts
- **Pact** - Alternative contract testing

### How It Works
```
┌────────────────┐        Contract        ┌────────────────┐
│    Consumer    │◄──────────────────────►│    Producer    │
│   (Frontend)   │                        │  (API Gateway) │
└────────────────┘                        └────────────────┘
         │                                        │
         │ Generate stub                          │ Verify contract
         ▼                                        ▼
    Consumer tests                          Producer tests
    against stub                            against contract
```

### Example Contract (Spring Cloud Contract DSL)
```groovy
Contract.make {
    description "should return item by id"
    request {
        method GET()
        url "/api/v1/items/1"
    }
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            name: "Test Item",
            price: 29.99
        ])
    }
}
```

## End-to-End Tests

### Purpose
Verify complete user flows work through the entire system.

### Tools
- **REST Assured** - HTTP API testing
- **JUnit 5** - Test framework

### Prerequisites
```bash
# E2E tests require all services running
docker compose up -d

# Verify services are healthy
docker compose ps
```

### What to Test
- Critical user journeys
- Authentication flows
- Business workflows
- Error scenarios

### Example
```java
class AuthE2ETest extends BaseE2ETest {

    @Test
    void userRegistrationAndLoginFlow() {
        // Register new user
        var registration = new RegistrationRequest(
            "test@example.com",
            "Password123!",
            "Test User"
        );

        given()
            .contentType(ContentType.JSON)
            .body(registration)
        .when()
            .post("/api/v1/auth/register")
        .then()
            .statusCode(201)
            .body("email", equalTo("test@example.com"));

        // Login with new credentials
        var login = new LoginRequest("test@example.com", "Password123!");

        var token = given()
            .contentType(ContentType.JSON)
            .body(login)
        .when()
            .post("/api/v1/auth/login")
        .then()
            .statusCode(200)
            .extract()
            .path("token");

        // Access protected resource
        given()
            .header("Authorization", "Bearer " + token)
        .when()
            .get("/api/v1/items")
        .then()
            .statusCode(200);
    }
}
```

### Running E2E Tests
```bash
# Ensure services are running
docker compose up -d

# Wait for health checks
docker compose ps  # All should be "healthy"

# Run E2E tests
cd e2e-tests
./mvnw.cmd clean test
```

### Scenario Coverage Checklist
Track which scenarios are covered:

#### Auth Flows
- [x] User registration (happy path)
- [x] User registration (duplicate email)
- [x] Login (valid credentials)
- [x] Login (invalid credentials)
- [ ] Token refresh
- [ ] Password reset
- [ ] Logout

#### Item Flows
- [x] List all items
- [x] Get item by ID
- [x] Get non-existent item
- [ ] Search items
- [ ] Filter by category

#### Shopping Flows (Future)
- [ ] Add to cart
- [ ] Update cart quantity
- [ ] Remove from cart
- [ ] Checkout
- [ ] Order history

## Test Data Management

### Unit Tests
- Create test data in each test
- Use builders or factories for complex objects

```java
// Use a builder for test data
var item = ItemTestBuilder.anItem()
    .withName("Test Item")
    .withPrice(BigDecimal.TEN)
    .build();
```

### Integration Tests
- Use @Sql or Testcontainers with init scripts
- Clean up after each test

```java
@Sql("/test-data/items.sql")
@Test
void findByCategory_returnsMatchingItems() {
    // Test uses data from items.sql
}
```

### E2E Tests
- Use docker compose init scripts
- Tests should be idempotent when possible
- Generate unique data to avoid conflicts

```java
// Generate unique email for each test run
var email = "test-" + UUID.randomUUID() + "@example.com";
```

## Coverage Configuration

### JaCoCo Configuration (pom.xml)
```xml
<plugin>
    <groupId>org.jacoco</groupId>
    <artifactId>jacoco-maven-plugin</artifactId>
    <version>0.8.12</version>
    <executions>
        <execution>
            <goals>
                <goal>prepare-agent</goal>
            </goals>
        </execution>
        <execution>
            <id>report</id>
            <phase>test</phase>
            <goals>
                <goal>report</goal>
            </goals>
        </execution>
        <execution>
            <id>check</id>
            <goals>
                <goal>check</goal>
            </goals>
            <configuration>
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

### Exclude from Coverage
```xml
<configuration>
    <excludes>
        <exclude>**/config/**</exclude>
        <exclude>**/*Application.*</exclude>
        <exclude>**/dto/**</exclude>
        <exclude>**/entity/**</exclude>
    </excludes>
</configuration>
```

## Test Naming Convention

### Java
```
methodName_stateUnderTest_expectedBehavior

Examples:
- findById_whenItemExists_returnsItem
- findById_whenItemNotFound_throwsException
- createOrder_withEmptyCart_throwsValidationException
- calculateTotal_withDiscount_appliesDiscountCorrectly
```

### File Naming
```
ClassNameTest.java          - Unit tests
ClassNameIntegrationTest.java - Integration tests
FeatureE2ETest.java         - E2E tests
```

## TDD Workflow

1. **Write failing test** (Red)
   ```java
   @Test
   void applyDiscount_withValidCode_reducesPrice() {
       // This fails because method doesn't exist
       var discounted = pricingService.applyDiscount(price, "SAVE10");
       assertThat(discounted).isEqualTo(new Money(90, EUR));
   }
   ```

2. **Write minimal implementation** (Green)
   ```java
   public Money applyDiscount(Money price, String code) {
       if ("SAVE10".equals(code)) {
           return new Money(price.amount().multiply(0.9), price.currency());
       }
       return price;
   }
   ```

3. **Refactor** (Refactor)
   ```java
   public Money applyDiscount(Money price, DiscountCode code) {
       var discount = discountRepository.findByCode(code)
           .orElseThrow(() -> new InvalidDiscountCodeException(code));
       return price.multiply(1 - discount.percentage());
   }
   ```

4. **Repeat** for next requirement

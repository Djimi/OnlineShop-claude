# Java Style Guide

Based on [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html) with project-specific additions for modern Java (21+).

## Formatting

### Indentation and Line Length
- **Indentation:** 4 spaces (no tabs)
- **Line limit:** 120 characters
- **Continuation indent:** 8 spaces

### Braces
K&R style (opening brace on same line):

```java
// Correct
public void process() {
    if (condition) {
        doSomething();
    } else {
        doOther();
    }
}

// Incorrect
public void process()
{
    if (condition)
    {
        doSomething();
    }
}
```

### Blank Lines
- One blank line between methods
- One blank line between logical sections within a method
- No blank line after opening brace or before closing brace

## Modern Java Features

### Use `var` When Type Is Obvious
```java
// Good - type is clear from right side
var items = new ArrayList<Item>();
var user = userRepository.findById(id);
var response = restTemplate.getForEntity(url, String.class);

// Bad - type not obvious, be explicit
var result = process(data);  // What type is result?

// Good - explicit when unclear
ProcessingResult result = process(data);
```

### Use Records for DTOs and Value Objects
```java
// Good - immutable, concise
public record ItemResponse(
    Long id,
    String name,
    BigDecimal price,
    String description
) {}

public record Money(BigDecimal amount, Currency currency) {
    public Money {
        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
    }
}
```

### Use Enhanced Switch
```java
// Good - expression form
String message = switch (status) {
    case PENDING -> "Waiting for processing";
    case APPROVED -> "Request approved";
    case REJECTED -> "Request rejected";
};

// Good - with yield for complex cases
int priority = switch (severity) {
    case CRITICAL, HIGH -> 1;
    case MEDIUM -> {
        log.info("Medium severity detected");
        yield 2;
    }
    case LOW -> 3;
};
```

### Use Pattern Matching
```java
// instanceof pattern matching
if (response instanceof SuccessResponse success) {
    return success.getData();
}

// Switch pattern matching (Java 21+)
return switch (shape) {
    case Circle c -> Math.PI * c.radius() * c.radius();
    case Rectangle r -> r.width() * r.height();
    case null -> 0;
};
```

### Use Sealed Classes for Domain Modeling
```java
public sealed interface PaymentResult
    permits PaymentSuccess, PaymentFailure, PaymentPending {
}

public record PaymentSuccess(String transactionId, Money amount)
    implements PaymentResult {}

public record PaymentFailure(String errorCode, String message)
    implements PaymentResult {}

public record PaymentPending(String referenceId)
    implements PaymentResult {}
```

### Use Text Blocks for Multi-line Strings
```java
// Good
String json = """
    {
        "name": "Test Item",
        "price": 29.99,
        "available": true
    }
    """;

// Bad
String json = "{\n" +
    "    \"name\": \"Test Item\",\n" +
    "    \"price\": 29.99\n" +
    "}";
```

## Naming Conventions

### Packages
- All lowercase, no underscores
- Reverse domain notation
```
com.onlineshop.auth.domain
com.onlineshop.items.infrastructure.repository
```

### Classes and Interfaces
- PascalCase
- Nouns for classes, adjectives or nouns for interfaces
```java
public class ItemRepository { }
public interface Searchable { }
public record UserCredentials() { }
```

### Methods
- camelCase
- Verbs or verb phrases
```java
public Item findById(Long id) { }
public void processOrder(Order order) { }
public boolean isValid() { }
public int getCount() { }
```

### Variables and Parameters
- camelCase
- Meaningful names, avoid abbreviations
```java
// Good
private final ItemRepository itemRepository;
private int retryCount;

// Bad
private final ItemRepository repo;
private int cnt;
```

### Constants
- SCREAMING_SNAKE_CASE
```java
public static final int MAX_RETRY_ATTEMPTS = 3;
public static final String DEFAULT_CURRENCY = "EUR";
```

## Class Structure

Order members consistently:
1. Static fields
2. Instance fields
3. Constructors
4. Public methods
5. Package-private methods
6. Protected methods
7. Private methods

```java
public class ItemService {
    // 1. Static fields
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    // 2. Instance fields
    private final ItemRepository itemRepository;
    private final EventPublisher eventPublisher;

    // 3. Constructors
    public ItemService(ItemRepository itemRepository, EventPublisher eventPublisher) {
        this.itemRepository = itemRepository;
        this.eventPublisher = eventPublisher;
    }

    // 4. Public methods
    public Item findById(Long id) {
        return itemRepository.findById(id)
            .orElseThrow(() -> new ItemNotFoundException(id));
    }

    // 5-7. Private/protected methods
    private void publishEvent(DomainEvent event) {
        eventPublisher.publish(event);
    }
}
```

## Comments

### When to Comment
- Tricky algorithms or business logic
- Configuration explanations
- Corner cases and workarounds
- Public API documentation (Javadoc)

### When NOT to Comment
```java
// Bad - obvious
// Get the user by ID
User user = userRepository.findById(id);

// Bad - restating the code
// Increment counter by one
counter++;

// Bad - obvious getter
/**
 * Gets the name.
 * @return the name
 */
public String getName() { return name; }
```

### Good Comments
```java
// Good - explains WHY, not WHAT
// Using insertion sort here because the list is nearly sorted (max 2 swaps needed)
insertionSort(nearlyOrderedList);

// Good - warns about corner case
// Redis may return null if key expired between exists() and get()
// Handle null explicitly rather than relying on cache hit
String value = redis.get(key);
if (value == null) {
    return fetchFromDatabase(key);
}

// Good - configuration explanation
@Value("${retry.max-attempts:3}")
private int maxAttempts;  // Retry limit for transient failures; increase for unreliable networks
```

## Exception Handling

### Use Specific Exceptions
```java
// Good
public Item findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new ItemNotFoundException(id));
}

// Bad - too generic
public Item findById(Long id) {
    return repository.findById(id)
        .orElseThrow(() -> new RuntimeException("Not found"));
}
```

### Don't Catch Generic Exceptions
```java
// Bad
try {
    process();
} catch (Exception e) {
    log.error("Error", e);
}

// Good - catch specific exceptions
try {
    process();
} catch (ItemNotFoundException e) {
    return notFound(e.getItemId());
} catch (ValidationException e) {
    return badRequest(e.getErrors());
}
```

## Spring-Specific Guidelines

### Constructor Injection (Required)
```java
// Good - constructor injection, immutable
@Service
public class ItemService {
    private final ItemRepository itemRepository;

    public ItemService(ItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }
}

// Bad - field injection
@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
}
```

### Use Lombok Judiciously
```java
// Good - reduces boilerplate
@RequiredArgsConstructor
@Service
public class ItemService {
    private final ItemRepository itemRepository;
}

// Good - for DTOs when not using records
@Data
@Builder
public class ItemRequest {
    private String name;
    private BigDecimal price;
}

// Prefer records over @Value for immutable data
public record ItemRequest(String name, BigDecimal price) {}
```

## Streams and Collections

### Use Streams for Transformation
```java
// Good
var activeUsers = users.stream()
    .filter(User::isActive)
    .map(User::getEmail)
    .toList();

// Bad - unnecessary stream for simple iteration
users.stream().forEach(user -> process(user));

// Good - use forEach directly
users.forEach(this::process);
```

### Prefer Immutable Collections
```java
// Good
public List<Item> getItems() {
    return List.copyOf(items);  // Immutable copy
}

// Or use unmodifiable
public List<Item> getItems() {
    return Collections.unmodifiableList(items);
}
```

## Optional Handling

```java
// Good - transform and return
public String getUserEmail(Long userId) {
    return userRepository.findById(userId)
        .map(User::getEmail)
        .orElse("unknown@example.com");
}

// Good - throw if required
public User getUser(Long userId) {
    return userRepository.findById(userId)
        .orElseThrow(() -> new UserNotFoundException(userId));
}

// Bad - isPresent() + get()
Optional<User> optUser = userRepository.findById(userId);
if (optUser.isPresent()) {
    return optUser.get().getEmail();
}

// Bad - Optional as field or parameter
private Optional<String> middleName;  // Use @Nullable or empty string instead
```

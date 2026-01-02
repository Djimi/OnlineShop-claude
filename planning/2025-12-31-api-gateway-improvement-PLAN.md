# API Gateway Service Improvement Plan

> Date: 2025-12-31
> Status: Pending implementation

## Overview

This plan improves the API Gateway service with better design patterns, testability, corner case handling, and comprehensive test coverage. Detailed subplans will be saved to `planning/api-gateway-improvement/`.

---

## Current State Analysis

### Files to Modify
| File | Issues |
|------|--------|
| [AuthValidationService.java](../api-gateway/src/main/java/com/onlineshop/gateway/service/AuthValidationService.java) | RestTemplate created inline (line 43), no interface, no circuit breaker |
| [AuthenticationFilter.java](../api-gateway/src/main/java/com/onlineshop/gateway/filter/AuthenticationFilter.java) | ObjectMapper created inline (line 31), no token sanitization |
| [pom.xml](../api-gateway/pom.xml) | Missing Resilience4j, Actuator, TestContainers |

### Key Problems Identified
1. **Testability**: RestTemplate created inline, no interfaces for mocking
2. **Resilience**: No circuit breaker when Auth service is down
3. **Security**: No token sanitization, potential header injection
4. **Coverage**: 0% test coverage (no tests exist)

---

## Implementation Tasks

### Phase 1: Code Improvements (Design Patterns & Abstraction)

- [ ] **1.1 Extract interfaces for dependency injection**
  - Create `TokenValidator` interface
  - Create `AuthServiceClient` interface
  - Create `TokenCacheManager` interface

- [ ] **1.2 Refactor AuthValidationService**
  - Inject RestTemplate via constructor (remove inline creation)
  - Split into smaller classes following SRP
  - Add circuit breaker with Resilience4j

- [ ] **1.3 Refactor AuthenticationFilter**
  - Inject ObjectMapper instead of creating inline
  - Add token sanitization before processing
  - Add header injection protection

- [ ] **1.4 Add new dependencies to pom.xml**
  - `spring-boot-starter-actuator` - Health checks & metrics
  - `resilience4j-spring-boot3` - Circuit breaker
  - `bucket4j-redis` - Rate limiting
  - `micrometer-registry-prometheus` - Metrics export
  - TestContainers dependencies for integration tests

- [ ] **1.5 Add resilience configuration**
  - Circuit breaker for Auth service (50% failure threshold)
  - Retry with exponential backoff (3 attempts)
  - Timeout limiter (3s)

- [ ] **1.6 Add rate limiting**
  - Token bucket algorithm with Bucket4j + Redis
  - Tiers: Anonymous (60/min), Authenticated (300/min)
  - Return 429 Too Many Requests when exceeded

- [ ] **1.7 Add metrics with Micrometer**
  - Cache hit/miss counters (L1, L2)
  - Auth service call latency histogram
  - Rate limit rejection counter
  - Circuit breaker state gauge

### Phase 2: Corner Cases

- [ ] **2.1 Token validation edge cases**
  - Empty/whitespace token rejection
  - Token length validation (max 8KB)
  - Null byte detection
  - Token expiry buffer (30s grace period)

- [ ] **2.2 Service unavailability**
  - Return 503 instead of 401 when Auth service down
  - Return 504 on timeout
  - Return 502 on bad gateway

- [ ] **2.3 Cache edge cases**
  - Handle Redis serialization errors gracefully
  - Add circuit breaker for Redis operations
  - Check `expiresAt` before returning cached tokens

- [ ] **2.4 Security hardening**
  - CRLF injection protection in headers
  - Sanitize username before adding to X-Username header

### Phase 3: Unit Testing

- [ ] **3.1 Create test infrastructure**
  - `TestDataFactory` for test fixtures
  - `MockRequestBuilder` for HTTP request mocking

- [ ] **3.2 AuthenticationFilterTest**
  - Path filtering (OPTIONS, /auth/**, /items/**)
  - Authorization header validation
  - Token validation success/failure
  - Header enrichment (X-User-Id, X-Username)
  - Error response format (RFC 9457)

- [ ] **3.3 AuthValidationServiceTest**
  - L1 cache hit/miss scenarios
  - L2 cache hit/miss with promotion
  - Redis failure fallback
  - Auth service call success/failure
  - Token hashing consistency
  - Cache TTL behavior

- [ ] **3.4 DTO Tests**
  - ErrorResponse factory methods
  - ValidateResponse serialization
  - JSON format validation

### Phase 4: Integration Testing

- [ ] **4.1 Setup TestContainers**
  - Redis container
  - MockServer for Auth service
  - Base integration test class

- [ ] **4.2 Full flow tests**
  - Request -> Auth validation -> Cache -> Response
  - Cache promotion (L2 to L1)
  - Redis unavailability fallback

- [ ] **4.3 Error scenario tests**
  - Auth service down -> 503
  - Auth service timeout -> 504
  - Invalid token -> 401
  - Missing header -> 401

---

## Files to Create

### New Source Files
```
api-gateway/src/main/java/com/onlineshop/gateway/
├── service/
│   ├── TokenValidator.java (interface)
│   ├── AuthServiceClient.java (interface)
│   └── impl/
│       └── DefaultAuthServiceClient.java
├── cache/
│   └── TokenCacheManager.java (interface)
├── validation/
│   └── TokenSanitizer.java
├── ratelimit/
│   ├── RateLimitConfig.java
│   └── RateLimitFilter.java
├── metrics/
│   └── GatewayMetrics.java
└── exception/
    ├── GatewayTimeoutException.java
    ├── ServiceUnavailableException.java
    └── TooManyRequestsException.java
```

### New Test Files
```
api-gateway/src/test/java/com/onlineshop/gateway/
├── filter/
│   ├── AuthenticationFilterTest.java
│   └── RateLimitFilterTest.java
├── service/
│   └── AuthValidationServiceTest.java
├── dto/
│   ├── ErrorResponseTest.java
│   └── ValidateResponseTest.java
├── validation/
│   └── TokenSanitizerTest.java
├── metrics/
│   └── GatewayMetricsTest.java
├── integration/
│   ├── com.onlineshop.auth.repository.base.BaseIntegrationTest.java
│   ├── AuthFlowIntegrationTest.java
│   ├── CacheIntegrationTest.java
│   └── RateLimitIntegrationTest.java
└── testutil/
    ├── TestDataFactory.java
    └── MockRequestBuilder.java
```

### Documentation Files
```
planning/api-gateway-improvement/
├── 01-CODE-IMPROVEMENTS.md
├── 02-CORNER-CASES.md
├── 03-UNIT-TESTING.md
└── 04-INTEGRATION-TESTING.md
```

---

## Dependencies to Add

```xml
<!-- Actuator for health checks & metrics -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Resilience4j for circuit breaker -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>

<!-- Bucket4j for rate limiting -->
<dependency>
    <groupId>com.bucket4j</groupId>
    <artifactId>bucket4j-redis</artifactId>
    <version>8.10.1</version>
</dependency>

<!-- Micrometer Prometheus for metrics -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- TestContainers -->
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>

<!-- MockServer -->
<dependency>
    <groupId>org.mock-server</groupId>
    <artifactId>mockserver-netty</artifactId>
    <version>5.15.0</version>
    <scope>test</scope>
</dependency>
```

---

## Configuration Changes

### application.yml additions
```yaml
# Resilience4j Circuit Breaker
resilience4j:
  circuitbreaker:
    instances:
      authService:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 30s
  retry:
    instances:
      authService:
        maxAttempts: 3
        waitDuration: 500ms
  timelimiter:
    instances:
      authService:
        timeoutDuration: 3s

# Rate Limiting
gateway:
  ratelimit:
    anonymous:
      requests-per-minute: 60
      burst: 10
    authenticated:
      requests-per-minute: 300
      burst: 50

# Actuator & Metrics
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: when-authorized
  metrics:
    tags:
      application: api-gateway
```

---

## Test Coverage Targets

| Component | Target | Priority |
|-----------|--------|----------|
| AuthenticationFilter | >95% | High |
| AuthValidationService | >95% | High |
| TokenSanitizer | >95% | High |
| RateLimitFilter | >90% | High |
| GatewayMetrics | >85% | Medium |
| ErrorResponse | >90% | Medium |
| ValidateResponse | >80% | Medium |

---

## Implementation Order

1. **Interface extraction** - Enables testing without full refactor
2. **Unit tests for existing code** - Safety net before changes
3. **RestTemplate injection** - Quick win for testability
4. **Token sanitization** - Security improvement
5. **Circuit breaker with Resilience4j** - Resilience improvement
6. **Rate limiting with Bucket4j** - Security & fairness
7. **Metrics with Micrometer/Prometheus** - Observability
8. **Integration tests with TestContainers** - Full confidence
9. **Documentation** - Save detailed plans to `planning/api-gateway-improvement/`

---

## Summary

This plan delivers:
- **Better testability** through interface extraction and dependency injection
- **Improved resilience** with circuit breakers, retries, and timeouts
- **Security hardening** via token sanitization and rate limiting
- **Observability** through Prometheus metrics
- **Comprehensive testing** with >90% unit test coverage and integration tests
- **Corner case handling** for all identified edge cases

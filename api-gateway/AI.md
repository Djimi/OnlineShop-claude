# API Gateway - Service Documentation

## Overview

The API Gateway is a Spring Cloud Gateway application that serves as the entry point for all client requests to the OnlineShop microservices. It provides:

- **Token Validation**: Multi-layer caching (L1: Caffeine, L2: Redis) for auth tokens
- **Rate Limiting**: Distributed rate limiting using Bucket4j and Redis
- **Resilience**: Circuit breakers, retries, and timeouts via Resilience4j
- **Observability**: Comprehensive metrics via Micrometer/Prometheus

## Technology Stack

- **Spring Boot 4.0.1** with Java 25
- **Spring Cloud Gateway (Web MVC)** - Non-reactive, supports virtual threads
- **RestClient** - Modern synchronous HTTP client (replaces RestTemplate)
- **Caffeine** - L1 local in-memory cache
- **Redis** - L2 distributed cache and rate limiting storage
- **Bucket4j** - Rate limiting
- **Resilience4j** - Circuit breakers, retries, and timeouts
- **Micrometer** - Metrics collection with Prometheus export

## Project Structure

```
api-gateway/
├── src/main/java/com/onlineshop/gateway/
│   ├── cache/              # Token caching (L1/L2)
│   ├── config/             # Spring configuration
│   ├── dto/                # Data transfer objects
│   ├── exception/          # Custom exceptions
│   ├── filter/             # Security and validation filters
│   ├── metrics/            # Metrics instrumentation
│   ├── ratelimit/          # Rate limiting filter
│   ├── service/            # Business logic services
│   └── validation/         # Token sanitization
└── src/test/java/          # Unit and integration tests
```

## Metrics and Observability

### Metrics Endpoint

Metrics are exposed via Spring Boot Actuator:
- **Endpoint**: `http://localhost:8080/actuator/prometheus`
- **Format**: Prometheus text format

### Available Metrics

| Metric Name | Type | Tags | Description |
|-------------|------|------|-------------|
| `gateway.cache.operations.total` | Counter | `layer`, `service`, `result` | Cache operation results (hits/misses) |
| `gateway.auth.service.latency` | Timer | - | Auth service call latency |
| `gateway.ratelimit.rejections.total` | Counter | - | Rate limit rejection count |

### Tag Conventions

The gateway uses **dimensional metrics with tags** (following Prometheus/Micrometer best practices):

#### Cache Metrics Tags

- **`layer`**: Cache layer identifier
  - `"l1"` - Local Caffeine cache (in-memory, nanosecond access)
  - `"l2"` - Distributed Redis cache (shared, millisecond access)

- **`service`**: Backend service identifier
  - `"auth"` - Authentication service
  - `"items"` - Items service (future)
  - Additional services can be added as needed

- **`result`**: Operation outcome
  - `"hit"` - Cache hit (data found)
  - `"miss"` - Cache miss (data not found)

### Tag Constants

Use the provided constants in `GatewayMetrics` to avoid typos:

```java
GatewayMetrics.LAYER_L1        // "l1"
GatewayMetrics.LAYER_L2        // "l2"
GatewayMetrics.SERVICE_AUTH    // "auth"
GatewayMetrics.SERVICE_ITEMS   // "items"
GatewayMetrics.RESULT_HIT      // "hit"
GatewayMetrics.RESULT_MISS     // "miss"
```

### Example Prometheus Queries

```promql
# Total cache operations for auth service
sum(gateway_cache_operations_total{service="auth"})

# Cache hit rate for auth service
sum(rate(gateway_cache_operations_total{service="auth",result="hit"}[5m])) /
sum(rate(gateway_cache_operations_total{service="auth"}[5m]))

# L1 vs L2 cache hit comparison
sum by (layer) (rate(gateway_cache_operations_total{service="auth",result="hit"}[5m]))

# All cache operations across all services
sum(rate(gateway_cache_operations_total[5m]))

# L1 cache hit rate (across all services)
sum(rate(gateway_cache_operations_total{layer="l1",result="hit"}[5m])) /
sum(rate(gateway_cache_operations_total{layer="l1"}[5m]))

# Auth service p99 latency
histogram_quantile(0.99, rate(gateway_auth_service_latency_bucket[5m]))

# Rate limit rejection rate
rate(gateway_ratelimit_rejections_total[5m])
```

### Adding Metrics for New Services

When adding cache metrics for a new service:

1. Use the existing `GatewayMetrics` methods
2. Add a new service constant if desired (e.g., `SERVICE_ORDERS`)
3. Pass the service name to the recording methods:

```java
// In your cache manager
metrics.recordCacheHit(GatewayMetrics.LAYER_L1, "orders");
metrics.recordCacheMiss(GatewayMetrics.LAYER_L2, "orders");
```

No code changes to `GatewayMetrics` are needed - the tag-based approach handles new services automatically.

## Configuration

### Application Properties

Key configuration properties:

```yaml
# Cache Configuration
gateway.cache.redis.ttl-seconds: 300  # Redis cache TTL (default: 5 minutes)

# Rate Limiting
gateway.ratelimit.anonymous.requests-per-minute: 60
gateway.ratelimit.anonymous.burst: 10
gateway.ratelimit.authenticated.requests-per-minute: 300
gateway.ratelimit.authenticated.burst: 50
```

### HTTP Client Configuration

The gateway uses Spring's `RestClient` with JDK's native `HttpClient` for Auth service communication.

**Timeout Configuration:**
- **Connection timeout**: 5 seconds (time to establish TCP connection)
- **Read timeout**: 5 seconds (time to read response after connection)
- **Overall timeout**: 3 seconds via Resilience4j `TimeLimiter`

**Timeout Precedence:**
The Resilience4j `TimeLimiter` (3s) typically triggers before the HTTP-level timeouts (5s), providing the primary timeout control. The HTTP-level timeouts serve as a safety net to prevent indefinite hangs if Resilience4j fails.

**Configuration in ResilienceConfig:**
```java
@Bean
public RestClient restClient() {
    HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
    requestFactory.setReadTimeout(Duration.ofSeconds(5));

    return RestClient.builder()
            .requestFactory(requestFactory)
            .build();
}
```

## Development

### Building the Project

```bash
# Build only
./mvnw clean install

# Build and run tests
./mvnw clean test

# Run the application
./mvnw spring-boot:run
```

### Running Tests

```bash
# Run all tests
./mvnw test

# Run specific test class
./mvnw test -Dtest=GatewayMetricsTest
```

### Local Development Setup

1. **Start Redis** (required for L2 cache and rate limiting):
   ```bash
   docker run -d -p 6379:6379 redis:alpine
   ```

2. **Start Auth Service** (required for token validation):
   ```bash
   cd ../Auth
   ./mvnw spring-boot:run
   ```

3. **Start API Gateway**:
   ```bash
   cd api-gateway
   ./mvnw spring-boot:run
   ```

4. **Access endpoints**:
   - Application: `http://localhost:8080`
   - Health: `http://localhost:8080/actuator/health`
   - Metrics: `http://localhost:8080/actuator/prometheus`

## Architecture Decisions

### Why Tag-Based Metrics?

The gateway uses **dimensional/tag-based metrics** instead of hierarchical naming:

**Old approach** (hierarchical):
```
gateway.cache.l1.hits
gateway.cache.l1.misses
gateway.cache.l2.hits
gateway.cache.l2.misses
```

**Current approach** (dimensional):
```
gateway.cache.operations.total{layer="l1", service="auth", result="hit"}
gateway.cache.operations.total{layer="l1", service="auth", result="miss"}
gateway.cache.operations.total{layer="l2", service="auth", result="hit"}
gateway.cache.operations.total{layer="l2", service="auth", result="miss"}
```

**Benefits:**
- ✅ **Future-proof**: Easy to add new services without code changes
- ✅ **Flexible querying**: Filter/aggregate by any dimension
- ✅ **Industry standard**: Aligns with Prometheus, Grafana, and modern observability tools
- ✅ **Better dashboards**: Single metric can be sliced multiple ways

**References:**
- [Micrometer Naming Conventions](https://docs.micrometer.io/micrometer/reference/concepts/naming.html)
- [Prometheus Metric Naming Best Practices](https://prometheus.io/docs/practices/naming/)

### Why Multi-Layer Caching?

- **L1 (Caffeine)**: Nanosecond access, reduces load on L2
- **L2 (Redis)**: Shared across gateway instances, reduces auth service load
- **Circuit breaker**: Protects against Redis failures

Benefits:
- Extremely low latency for hot tokens (L1 hits)
- Consistency across gateway instances (L2)
- Graceful degradation if Redis fails

### Why RestClient Over RestTemplate?

The gateway uses **RestClient** instead of the legacy **RestTemplate** for HTTP communication:

**Migration rationale:**
- **Deprecation**: RestTemplate is deprecated in Spring Framework 7.0, removal planned for 8.0
- **Modern API**: RestClient offers a more fluent, readable interface
- **Virtual threads optimized**: Built for Spring Boot 4.0.1 with Java 25 virtual threads
- **No additional dependencies**: Works with JDK's native `HttpClient` (Java 11+)

**API Comparison:**

RestTemplate (old):
```java
HttpEntity<Void> entity = new HttpEntity<>(headers);
ResponseEntity<ValidateResponse> response = restTemplate.exchange(
    url, HttpMethod.GET, entity, ValidateResponse.class);
```

RestClient (new):
```java
ValidateResponse response = restClient.get()
    .uri(url)
    .header("Authorization", "Bearer: " + token)
    .retrieve()
    .body(ValidateResponse.class);
```

**Benefits:**
- ✅ Future-proof (aligns with Spring roadmap)
- ✅ Less boilerplate code
- ✅ Method chaining improves readability
- ✅ Better integration with modern Spring features

**References:**
- [The State of HTTP Clients in Spring](https://spring.io/blog/2025/09/30/the-state-of-http-clients-in-spring/)
- [Spring Boot 3.2: Replace Your RestTemplate With RestClient](https://dzone.com/articles/spring-boot-32-replace-your-resttemplate-with-rest)

## Troubleshooting

### Metrics Not Appearing

1. Check actuator is enabled: `management.endpoints.web.exposure.include=prometheus`
2. Verify endpoint: `curl http://localhost:8080/actuator/prometheus | grep gateway_`
3. Generate traffic to trigger metric creation (metrics are created on first use)

### Redis Connection Issues

- Check Redis is running: `redis-cli ping`
- Check connection properties in `application.yml`
- Monitor circuit breaker state (logs will show if circuit is open)

### Rate Limiting Not Working

- Verify Redis is accessible
- Check rate limit configuration
- Ensure requests have proper headers (`X-User-Id` for authenticated users)

## References

- [Spring Cloud Gateway Documentation](https://docs.spring.io/spring-cloud-gateway/reference/)
- [Micrometer Documentation](https://docs.micrometer.io/)
- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Bucket4j Documentation](https://github.com/bucket4j/bucket4j)

# API Gateway - Service Documentation

## Overview

The API Gateway is a Spring Cloud Gateway application that serves as the entry point for all client requests to the OnlineShop microservices. It provides:

- **Token Validation**: It hits Auth service and also uses multi-layer caching (L1: Caffeine, L2: Redis) to store the respective results (to save latency and reduce load on Auth service)
- **Rate Limiting**: Distributed rate limiting using Bucket4j and Redis
- **Resilience**: Circuit breakers, retries, and timeouts via Resilience4j
- **Observability**: Comprehensive metrics via Micrometer/Prometheus

## Contracts

The gateway exposes the following endpoints to clients:
  - **All service endpoints are proxied through the gateway.**:
  - /items/** endpoint proxies to `/api/v1/items/**` of Items service (for product listing, details, CRUD, etc.)
  - Requests to /items/** require a valid authentication token.
  - /auth/** proxies to `/api/v1/auth/**` of Auth service (for login, logout, token validation, etc.)
  - Requests to /auth/** do not require authentication.
  - Auth-token must be provided in the `Authorization` header as a Bearer token for protected endpoints. Currently, only opaque tokens are supported.
  - It uses port `10000`.

## Technology Stack

- **Spring Boot 4.X.X** with Java 25
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

### Resiliency
- Use circuit breakers, retries, and timeouts to handle failures gracefully
- The service MUST boot and response even if Redis is down as it is only a cache layer
- The service should continue to serve the requests which are already cached even if Auth service is down, the other requests should fail fast
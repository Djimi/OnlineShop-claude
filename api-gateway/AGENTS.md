# API Gateway - Service Documentation

## Overview

The API Gateway is the entry point to OnlineShop services. It routes traffic, enforces auth, and adds resilience.

| Property   | Value                                  |
|------------|----------------------------------------|
| Port       | 10000                                  |
| Tech Stack | Spring Cloud Gateway, Redis, Caffeine  |
| Location   | `/api-gateway`                         |
| Database   | None (uses Redis for caching/limits)   |

## Responsibilities

- **Routing**: `/api/v1/auth/**` → Auth, `/api/v1/items/**` → Items.
- **Authentication**: Bearer token required for `/api/v1/items/**`.
- **Public info endpoint**: `/api/product-info` → static product info (no auth, no downstream call).
- **Token caching**: L1 Caffeine + L2 Redis for validation results.
- **Rate limiting**: Bucket4j + Redis.
- **Resilience/observability**: Resilience4j + Micrometer/Prometheus.

## Contracts (Examples)

- Public: `/api/v1/auth/**`
- Public: `/api/product-info` (static info endpoint; unversioned by design)
- Protected: `/api/v1/items/**`

Example:
```bash
curl -H "Authorization: Bearer <token>" \
  http://localhost:10000/api/v1/items
```

## Authentication Flow (Example)

```
1. Request hits gateway
2. /auth/** → forward without auth
3. /items/** → require Authorization header
4. Validate token: L1 → L2 → Auth service
5. On success → add X-User-Id, X-Username headers → forward
6. On failure → 401 Unauthorized
```

## Token Validation Caching

- **L1 (Caffeine)**: Nanosecond local hits.
- **L2 (Redis)**: Shared cache across instances.

Example metric (tag-based):
```
gateway.cache.operations.total{layer="l1", service="auth", result="hit"}
```

## Technology Stack

- **Spring Boot 4.X.X** with Java 25
- **Spring Cloud Gateway (Web MVC)** - Non-reactive, supports virtual threads
- **RestClient** - Modern synchronous HTTP client
- **Caffeine** - L1 local in-memory cache
- **Redis** - L2 distributed cache + rate limiting store
- **Bucket4j** - Rate limiting
- **Resilience4j** - Circuit breakers, retries, timeouts
- **Micrometer** - Metrics collection with Prometheus export
- **Lombok** - Compile-time annotations (provided scope)

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

## Resilience Notes

- Service boots even if Redis is down (cache is optional).
- Cached tokens continue to work during Auth outages; uncached tokens fail fast.
- Rate limiting can be disabled via `gateway.ratelimit.enabled=false` (useful for tests).

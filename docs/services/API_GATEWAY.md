# API Gateway Service

## Overview

| Property   | Value                                  |
|------------|----------------------------------------|
| Port       | 10000                                  |
| Tech Stack | Spring Cloud Gateway, Redis, Caffeine  |
| Location   | `/api-gateway`                         |
| Database   | None (uses Redis for caching)          |

## Responsibilities

1. **Routing** - Route requests to appropriate backend services
2. **CORS** - Handle Cross-Origin Resource Sharing centrally
3. **Authentication** - Validate JWT tokens before forwarding
4. **Token Caching** - Two-layer cache for token validations (Caffeine L1 + Redis L2)
5. **Rate Limiting** - (Future) Protect services from overload

## Key Files

| Purpose         | Location                                                         |
|-----------------|------------------------------------------------------------------|
| Configuration   | `api-gateway/src/main/resources/application.yml`                 |
| Auth Filter     | `api-gateway/src/main/java/.../filter/AuthenticationFilter.java` |
| Token Cache     | `api-gateway/src/main/java/.../service/AuthValidationService.java` |
| Cache Config    | `api-gateway/src/main/java/.../config/CacheConfig.java`          |

## API Endpoints

All routes are prefixed with `/api/v1/`:

| Method | Path                | Target Service | Auth Required |
|--------|---------------------|----------------|---------------|
| *      | `/api/v1/auth/**`   | Auth Service   | No            |
| *      | `/api/v1/items/**`  | Items Service  | Yes           |

> **Note:** All `/items/**` endpoints require authentication. See `AuthenticationFilter.java`.

## Token Validation Caching

Caches **token validation results** (not API responses) to avoid calling Auth service on every request:

- **L1 (Caffeine):** Local in-memory, nanosecond access
- **L2 (Redis):** Distributed across gateway instances

Flow: L1 → L2 → Auth Service (on cache miss)

Configuration: See `application.yml` under `gateway.cache` section.

## Authentication Flow

```
1. Request arrives at Gateway
2. AuthenticationFilter checks path
3. If /auth/** → skip auth, forward directly
4. If /items/** → require Authorization header
5. Validate token (check L1 cache → L2 cache → Auth Service)
6. If valid → add X-User-Id, X-Username headers → forward
7. If invalid → 401 Unauthorized
```

## Running Locally

### With Docker Compose (Recommended)
```bash
docker compose up -d api-gateway
```

### Standalone
```bash
cd api-gateway
JAVA_HOME="/c/Program Files/Java/jdk-25" ./mvnw.cmd spring-boot:run
```

Requires: Redis, Auth Service, Items Service running.

## Health Check

```bash
curl http://localhost:10000/actuator/health
```

## Common Issues

### CORS Errors
1. Check origin is in allowed list (see `application.yml`)
2. Verify requests go through gateway, not direct to services
3. Check browser network tab for actual error

### Token Cache Not Working
1. Verify Redis is running: `docker compose ps redis`
2. Check Redis connectivity: `redis-cli ping`

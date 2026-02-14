# CLAUDE.md - Auth Service Guide

> This file helps Claude understand and work efficiently with the Auth microservice.

---

## Quick Reference

| Property | Value             |
|----------|-------------------|
| Port | 9001              |
| Language | Java 25           |
| Framework | Spring Boot 4.X.X |
| Database | PostgreSQL        |
| Build Tool | Maven             |

## Service Overview

The Auth service handles user authentication and session management:
- User registration
- User login
- Session token validation
- Session management

## Project Structure

```
Auth/
├── src/main/java/com/onlineshop/auth/
│   ├── controller/     # REST endpoints
│   ├── service/        # Business logic
│   ├── repository/     # Data access
│   ├── entity/         # JPA entities (User, Session)
│   ├── dto/            # Request/Response DTOs
│   ├── exception/      # Custom exceptions & handlers
│   └── config/         # Security configuration
├── init-db/            # Database initialization scripts
└── pom.xml
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | User login |
| GET | `/api/v1/auth/validate` | Validate session token |

## Gateway Contract (Example)

- Gateway proxies `/api/v1/auth/**` to this service with no auth required.

Example:
```bash
curl http://localhost:10000/api/v1/auth/validate
```

## Database

- Schema: [init-db/01-schema.sql](./init-db/01-schema.sql)
- Seed data: [init-db/02-seed-data.sql](./init-db/02-seed-data.sql)

## Configuration

Main configuration: [src/main/resources/application.yml](./src/main/resources/application.yml)

### DB Troubleshooting Toggle

Hibernate query diagnostics are now disabled by default.

Enable diagnostics with one startup argument:

```bash
--spring.profiles.active=db-troubleshooting
```

Optional slow-query threshold (milliseconds):

```bash
--auth.troubleshooting.hibernate.slow-query-threshold-ms=100
```

Optional datasource acquire threshold for warning logs (milliseconds):

```bash
--auth.troubleshooting.datasource.acquire-slow-threshold-ms=2
```

When the `db-troubleshooting` profile is active, the service enables:
- Hibernate statistics generation
- Hibernate session event logging
- Slow query logging (`LOG_QUERIES_SLOWER_THAN_MS`)
- Verbose Hibernate SQL/statistics log levels
- Hikari internal pool diagnostics (`com.zaxxer.hikari.*` at `TRACE`)
- Datasource acquisition timing logs with pool state (`active/idle/waiting/total`) and sub-millisecond precision
- Console logs include log level and logger name for faster troubleshooting

To disable diagnostics, start the service without the profile argument.

## Resilience & Caching
- No caching implemented currently

## Performance Test Notes
- Smoke performance test (`tests/performance/smoke-1vu.js`) uses 7 seeded users created in setup.
- Each smoke iteration keeps a fixed `1:1:8` operation ratio:
- 1 register, 1 login, 8 validate.
- Validate flow is `7` requests with distinct valid tokens and `1` request with an invalid token.
- Invalid-token validate is expected to return HTTP `200` with response field `valid=false`.


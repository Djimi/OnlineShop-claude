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

### Hibernate Troubleshooting Toggle

Hibernate query diagnostics are now disabled by default.

Enable diagnostics with one startup argument:

```bash
--spring.profiles.active=hibernate-troubleshooting
```

Optional slow-query threshold (milliseconds):

```bash
--auth.troubleshooting.hibernate.slow-query-threshold-ms=100
```

When the `hibernate-troubleshooting` profile is active, the service enables:
- Hibernate statistics generation
- Hibernate session event logging
- Slow query logging (`LOG_QUERIES_SLOWER_THAN_MS`)
- Verbose Hibernate SQL/statistics log levels

To disable diagnostics, start the service without the profile argument.

## Resilience & Caching
- No caching implemented currently

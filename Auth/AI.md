# CLAUDE.md - Auth Service Guide

> This file helps Claude understand and work efficiently with the Auth microservice.

---

## Quick Reference

| Property | Value             |
|----------|-------------------|
| Port | 8081              |
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



## Database

- Schema: [init-db/01-schema.sql](./init-db/01-schema.sql)
- Seed data: [init-db/02-seed-data.sql](./init-db/02-seed-data.sql)

## Configuration

Main configuration: [src/main/resources/application.yml](./src/main/resources/application.yml)

## Parent Documentation

For project-wide guidelines, see the root [CLAUDE.md](../CLAUDE.md):
- [Core Principles](../docs/CORE_PRINCIPLES.md) - Development philosophy, code quality rules
- [API Design](../docs/API_DESIGN.md) - API standards, error handling
- [Testing Strategy](../docs/TESTING_STRATEGY.md) - Testing requirements
- [Git Workflow](../docs/GIT_WORKFLOW.md) - Branch naming, commit format

## Known TODOs

See [TODO.md](./TODO.md) for pending improvements.

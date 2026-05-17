# Items Service - Service Documentation

## Quick Reference

| Property | Value |
|----------|-------|
| Port | 9000 |
| Language | Java 25 |
| Framework | Spring Boot 4.X.X |
| Database | PostgreSQL |
| Build Tool | Maven |

## Service Overview

The Items service manages product inventory and search:
- Item creation, updates, and deletion
- Item lookup by id
- Item listing and description search

## Project Structure

```
Items/
├── src/main/java/com/onlineshop/items/
│   ├── application/     # Use cases, commands, queries, DTOs, events, mappers
│   ├── domain/          # Domain models, value objects, exceptions, and interfaces
│   ├── infrastructure/  # Persistence and integrations
│   ├── web/             # REST controllers and request/response DTOs
│   └── ItemsApplication.java
├── init-db/             # Database initialization scripts
└── pom.xml
```

## API Endpoints

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/v1/items` | List all items |
| GET | `/api/v1/items/{id}` | Get item by id |
| GET | `/api/v1/items/search?description=...` | Search items by description |
| POST | `/api/v1/items` | Create new item |
| PUT | `/api/v1/items/{id}` | Update existing item |
| DELETE | `/api/v1/items/{id}` | Delete item |

## Database

- Schema: [init-db/01-schema.sql](./init-db/01-schema.sql)
- Seed data: [init-db/02-data.sql](./init-db/02-data.sql)

## Testing

**48 tests total** — 0 failures (Phase 4 complete).

| Layer | Test File | Approach | Tests |
|-------|-----------|----------|-------|
| Domain | `domain/ItemTest`, `ValueObjectTest` | JUnit 5 + AssertJ (no Spring) | 37 |
| Application | `application/usecase/UseCaseIntegrationTest` | `@SpringBootTest(NONE)` + Testcontainers | 6 |
| Application | `application/usecase/SearchItemsUseCaseTest` | JUnit 5 + Mockito | 4 |
| Web | `web/controller/ItemsControllerE2eTest` | `@SpringBootTest(RANDOM_PORT)` + Testcontainers + `RestTemplate` | 7 |

> **Important:** Spring Boot 4.0.2 removed `@WebMvcTest`, `@MockBean`, and `MockMvc`. Controller testing uses `@SpringBootTest(webEnvironment = RANDOM_PORT)` + Testcontainers. For corner cases not covered by E2E, use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito.MockitoBean`.

## Configuration

Main configuration: [src/main/resources/application.yml](./src/main/resources/application.yml)

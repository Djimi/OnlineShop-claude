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
- Item creation and updates
- Item lookup by id
- Item listing and description search

## Project Structure

```
Items/
├── src/main/java/com/onlineshop/items/
│   ├── application/     # Use cases, commands, queries, DTOs
│   ├── domain/          # Domain models and interfaces
│   ├── infrastructure/  # Persistence and integrations
│   ├── web/             # REST controllers
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

## Database

- Schema: [init-db/01-schema.sql](./init-db/01-schema.sql)
- Seed data: [init-db/02-data.sql](./init-db/02-data.sql)

## Configuration

Main configuration: [src/main/resources/application.yml](./src/main/resources/application.yml)

## Documentation Sync
- Last sync: 2026-02-01 (Gateway added `/api/product-info` and rate limit test toggle; Items unchanged)

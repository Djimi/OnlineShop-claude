# CLAUDE.md - Project Guide for Claude

> This file helps Claude understand and work efficiently with the OnlineShop project.

## Project Overview

**Type:** Microservices-based e-commerce learning platform
**Purpose:** Learn technologies, architectural patterns, and UI development
**Owner:** Senior Java Developer learning frontend and advanced patterns

## Quick Reference

### Services & Ports

| Service       | Port  | Tech Stack                             | Description                               |
|---------------|-------|----------------------------------------|-------------------------------------------|
| API Gateway   | 10000 | Spring Cloud Gateway, Redis, Caffeine  | Central routing, caching, auth validation |
| Auth Service  | 9001  | Spring Boot 4, PostgreSQL              | Authentication, JWT tokens, sessions      |
| Items Service | 9000  | Spring Boot 4, PostgreSQL              | Product inventory CRUD                    |
| Frontend      | 5173  | React 19, TypeScript, Vite, Tailwind   | User interface                            |

### Essential Commands

```bash
# Start all services (from root)
docker compose up -d

# Stop all services
docker compose down

# Stop all services AND remove volumes (DATABASE DATA WILL BE LOST)
# DANGER: Only run when explicitly requested and confirmed by user
docker compose down -v

# Apply code changes to a service (MUST build JAR first, then rebuild container)
# Run from service directory (e.g., Items/, Auth/, api-gateway/):
JAVA_HOME="/c/Program Files/Java/jdk-25" ./mvnw.cmd clean package -DskipTests
# Then from root, rebuild the Docker image:
docker compose up -d --build <service-name>

# Run unit + integration tests (from service directory)
JAVA_HOME="/c/Program Files/Java/jdk-25" ./mvnw.cmd clean test

# Run e2e tests (from e2e-tests/, requires docker compose up first)
cd e2e-tests && JAVA_HOME="/c/Program Files/Java/jdk-25" ./mvnw.cmd clean test

# Frontend development
cd frontend && npm run dev

# Frontend build
cd frontend && npm run build
```

> **Important:** When making code changes to backend services, you MUST:
> 1. Build the JAR with Maven from the service directory (`./mvnw.cmd clean package`)
> 2. Rebuild the Docker image from root (`docker compose up -d --build <service-name>`)
>
> Without step 1, the Docker image will use the old JAR and changes won't apply.

## Core Principles

### Development Philosophy

1. **Test-Driven Development (TDD)** - Write tests first, then implementation
2. **Domain-Driven Design (DDD)** - Model domain explicitly, use ubiquitous language
3. **High Abstraction** - Program to interfaces, use design patterns, leverage generics
4. **Latest Technologies** - Always use newest stable versions (Java 25, Spring Boot 4, React 19)
5. **Modern Java Features** - Virtual threads, enhanced switch, var, records, sealed classes

### Code Quality Rules

- **Test coverage:** >90% for unit tests (measured by JaCoCo)
- **Comments:** Only for tricky logic, configs, corner cases. Never obvious comments like "// get the user"
- **Corner cases:** Always handle DB/Redis/broker down scenarios
- **Security:** Follow OWASP guidelines, never log sensitive data

### Design Patterns

Standard design patterns (Repository, Factory, Strategy, etc.) are applied where appropriate throughout the codebase to enhance code maintainability, testability, and adherence to SOLID principles.

### When Making Changes

1. **Update all related files:** docker compose, tests, architecture docs, ADRs if needed
2. **Run all tests:** Unit, integration, e2e before considering done
3. **E2E prerequisite:** Check if docker compose is running, start if not

## Git Workflow

### Branch Naming
- Features: `feature/<description>` (e.g., `feature/add-cart-service`)
- Bug fixes: `bug/<description>` (e.g., `bug/fix-auth-token-refresh`)
- **Never commit directly to main**

### Commit Message Format (Conventional Commits)
```
<type>(<scope>): <description>

[optional body]

Types: feat, fix, refactor, test, docs, chore, perf
Scopes: auth, items, gateway, frontend, e2e, docs
```

Examples:
```
feat(auth): add password reset endpoint
fix(items): correct price calculation for bulk discounts
test(e2e): add checkout flow scenarios
```

## Architecture

### API Design
- **Versioning:** Path-based `/api/v1/resource`
- **Error format:** RFC 9457 Problem Details
- **Response format:** JSON with consistent structure

### Patterns in Use
- API Gateway Pattern (routing, caching, auth validation)
- Two-layer caching (Caffeine L1 + Redis L2)
- Virtual threads for concurrency (not reactive)

### Patterns to Implement (Learning Goals)
See [TECHNOLOGIES_TO_EXPLORE.md](./TECHNOLOGIES_TO_EXPLORE.md) for full list:
- CQRS, Event Sourcing, SAGA, Transactional Outbox
- Kubernetes, WebSockets, Kafka

## Documentation Structure

```
docs/
├── architecture/
│   ├── OVERVIEW.md          # System architecture
│   ├── PATTERNS.md          # DDD, patterns in use
│   └── adr/                 # Architecture Decision Records
├── style-guides/
│   ├── JAVA.md              # Java code style (Google-based)
│   └── TYPESCRIPT.md        # React/TypeScript style
├── testing/
│   └── STRATEGY.md          # Testing pyramid, coverage
└── services/
    ├── API_GATEWAY.md
    ├── AUTH_SERVICE.md
    ├── ITEMS_SERVICE.md
    └── FRONTEND.md
```

## Style Guides

### Java (Summary)
- Based on [Google Java Style Guide](https://google.github.io/styleguide/javaguide.html)
- 120 character line limit
- 4 spaces indentation (no tabs)
- Full details: [docs/style-guides/JAVA.md](./docs/style-guides/JAVA.md)

### TypeScript/React (Summary)
- Functional components with hooks
- TypeScript strict mode
- Full details: [docs/style-guides/TYPESCRIPT.md](./docs/style-guides/TYPESCRIPT.md)

## Testing Strategy

### Test Types & Coverage

| Type        | Tool                           | Coverage Target | Scope                      |
|-------------|--------------------------------|-----------------|----------------------------|
| Unit        | JUnit 5 + Mockito              | >90%            | Single class               |
| Integration | Spring Test + Testcontainers   | Measured        | Service layer with real DB |
| Contract    | Spring Cloud Contract (future) | N/A             | API contracts              |
| E2E         | REST Assured                   | Scenario-based  | Full system                |

### Running Tests

```bash
# Unit + Integration (per service)
./mvnw.cmd clean test

# E2E (from e2e-tests/, requires docker compose up)
./mvnw.cmd clean test

# With coverage report
./mvnw.cmd clean test jacoco:report
# Report at: target/site/jacoco/index.html
```

## Logging Standards

- **Format:** Structured JSON in production
- **Levels:** ERROR (alerts), WARN (investigate), INFO (audit), DEBUG (dev)
- **Never log:** Passwords, tokens, PII, credit cards
- **Always log:** Request IDs, user IDs (not emails), business events, errors with context

## Error Handling

Use RFC 9457 Problem Details format:
```json
{
  "type": "https://api.onlineshop.com/errors/item-not-found",
  "title": "Item Not Found",
  "status": 404,
  "detail": "Item with ID 123 does not exist",
  "instance": "/api/v1/items/123"
}
```

## Efficiency Guidelines for Claude

1. **Read before edit** - Always read files before modifying
2. **Batch related changes** - Update all affected files together
3. **Use search efficiently** - Glob for file patterns, Grep for content
4. **Parallel operations** - Run independent commands in parallel
5. **Verify changes** - Run tests after modifications
6. **Quality over speed** - Take time to do it right

## Communication Style

When explaining concepts:
- Be practical with real code examples
- Go low-level with implementation details
- Avoid vague or theoretical-only explanations
- Cover edge cases and error scenarios

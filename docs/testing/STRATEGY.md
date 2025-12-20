# Testing Strategy

## Philosophy

We follow **Test-Driven Development (TDD)**: write a failing test first, implement the minimal code to pass, then refactor. Tests are not an afterthought—they drive design decisions and serve as living documentation.

Tests provide confidence to refactor, deploy, and evolve the system. We optimize for **fast feedback loops**: unit tests run in milliseconds, integration tests in seconds, E2E tests in minutes. The testing pyramid reflects this—many fast tests at the bottom, few slow tests at the top.

## Testing Pyramid

```
                ▲
               / \           E2E Tests
              /   \          (Few, Slow, High Confidence)
             /─────\
            /       \        Integration Tests
           /         \       (Some, Medium Speed)
          /───────────\
         /             \     Unit Tests
        /               \    (Many, Fast, Focused)
       ─────────────────────
```

## Coverage Targets

| Test Type   | Target | Scope                          | Tools                        |
|-------------|--------|--------------------------------|------------------------------|
| Unit        | >90%   | Single class/function          | JUnit 5, Mockito, AssertJ    |
| Integration | —      | Multiple components, real DB   | Spring Test, Testcontainers  |
| Contract    | —      | API contracts between services | Spring Cloud Contract (future) |
| E2E         | —      | Critical user journeys         | REST Assured                 |

## Test Categories

### Unit Tests
Test business logic in isolation. Mock all dependencies. These are your primary safety net—fast, focused, and numerous. Test edge cases, validation rules, and error handling. Don't test simple getters/setters or framework code.

### Integration Tests
Verify components work together with real dependencies. Use Testcontainers for PostgreSQL and Redis—never H2 or in-memory substitutes. Test repository queries, controller request handling, and database constraints. These catch issues unit tests miss.

### Contract Tests (Future)
When services multiply, contract tests prevent breaking changes between API consumers and producers. The consumer defines expectations; the producer verifies compliance. Critical for microservices independence.

### E2E Tests
Validate complete user journeys through the running system. Expensive to write and maintain—reserve for critical paths only: authentication flows, core business transactions, payment processing. Run against `docker compose up`.

## Key Decisions

### Why 90% Coverage Target (Not 100%)
100% creates perverse incentives—testing trivial code to hit a number. 90% ensures meaningful coverage while allowing pragmatic exclusions (DTOs, configuration classes, Spring Boot main classes).

### Why Testcontainers Over H2
H2 lies. It behaves differently than PostgreSQL for JSON columns, array types, and query edge cases. Testcontainers runs the real database—if tests pass, production will work. The few seconds of startup time prevent hours of debugging.

### Why REST Assured for E2E
Fluent API reads like documentation. Given/When/Then structure mirrors BDD. Built-in JSON path assertions. No browser overhead for API testing.

### Coverage Exclusions
Excluded from coverage measurement (see `pom.xml` JaCoCo config):
- `**/config/**` — Spring configuration classes
- `**/*Application.*` — Main class bootstrap
- `**/dto/**` — Data transfer objects (no logic)
- `**/entity/**` — JPA entities (framework-managed)

## Test Naming Convention

```
methodName_stateUnderTest_expectedBehavior

Examples:
- findById_whenItemExists_returnsItem
- findById_whenItemNotFound_throwsException
- calculateTotal_withDiscount_appliesCorrectPercentage
```

File naming:
- `*Test.java` — Unit tests
- `*IntegrationTest.java` — Integration tests
- `*E2ETest.java` — End-to-end tests

## TDD Workflow

1. **Red** — Write a failing test for the next requirement
2. **Green** — Write minimal code to make it pass
3. **Refactor** — Improve design while keeping tests green
4. **Repeat**

Resist the urge to write production code without a failing test first. The discipline pays dividends in design quality and regression safety.

## References

| What | Where |
|------|-------|
| Running tests | [CLAUDE.md](../../CLAUDE.md) — Essential Commands |
| Unit test example | `Items/src/test/java/**/ItemServiceTest.java` |
| Integration test example | `Items/src/test/java/**/*IntegrationTest.java` |
| E2E tests | `e2e-tests/src/test/java/` |
| JaCoCo configuration | Service `pom.xml` files — search for `jacoco-maven-plugin` |
| Test data utilities | `*/src/test/java/**/testutil/` |

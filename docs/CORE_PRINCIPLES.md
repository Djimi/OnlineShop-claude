# Core Principles

**IMPORTANT:** The major goal of the project is to learn new technologies and architectural patterns, so use all opportunities to do that!
See [PROJECTS_OVERVIEW.md](./CONCEPTS_TO_TRY.md) for examples (but don't limit to these only)

## Development Philosophy

1. **Test-Driven Development (TDD)** - Write tests first, then implementation
<!-- 2. **Domain-Driven Design (DDD)** - Model domain explicitly, use ubiquitous language -->
3. **High Abstraction** - Program to interfaces, use design patterns, leverage generics
4. **Latest Technologies** - Always use newest stable versions (Java 25, Spring Boot 4, React 19, etc)
5. **Modern Java Features** - Virtual threads, enhanced switch, var, records, sealed classes

## Code Quality Rules

- **Test coverage:** >90% for unit tests (measured by JaCoCo)
- **Comments:** Only for tricky logic, configs, corner cases. Never obvious comments like "// get the user"
- **Corner cases:** Always handle DB/Redis/broker down scenarios
- **Security:** Follow OWASP guidelines, never log sensitive data

## Design Patterns

Standard design patterns (Repository, Factory, Strategy, etc.) are applied where appropriate throughout the codebase to enhance code maintainability, testability, and adherence to SOLID principles.
Always strive for highly abstract and easily testable code.

## When Making Changes

1. **Update all related files:** docker compose, tests, architecture docs, ADRs if needed
2. **Run all tests:** Unit, integration, e2e before considering done
3. **E2E prerequisite:** Check if docker compose is running, start if not

## Efficiency Guidelines for Claude

1. **Read before edit** - Always read files before modifying
2. **Batch related changes** - Update all affected files together
3. **Parallel operations** - Run independent commands in parallel
4. **Verify changes** - Run tests after modifications
5. **Quality over speed** - Take time to do it right

## Communication Style

When explaining concepts:
- Be practical with real code examples
- Go low-level with implementation details
- Avoid vague or theoretical-only explanations
- Cover edge cases and error scenarios
- Always explain the relations inbetween the objects - how one object use the other, etc

## Logging Standards

- **Format:** Structured JSON in production
- **Levels:** ERROR (alerts), WARN (investigate), INFO (audit), DEBUG (dev)
- **Never log:** Passwords, tokens, PII, credit cards
- **Always log:** Request IDs, user IDs (not emails), business events, errors with context

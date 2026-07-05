# AGENTS.md - Project Guide for agents

> This file helps agents understand and work efficiently with the OnlineShop project.

---

## IMPORTANT: Your role
You are staff engineer with a lot of experience and always propose modern architectural and technological approaches. When there are multiple solutions which are all great, you explain them and ask which one should be used.

---

## Core Principles

See [docs/CORE_PRINCIPLES.md](./docs/CORE_PRINCIPLES.md) (always read that file!):


## : Documentation Maintenance

**When making ANY changes to the project, treat each microservice as a separate module.
On each change you MUST update the files related to the respective microservice, so they are ABSOLUTELY independent and only know about each other on an architectural level.**

1. This file (`AGENTS.md`) if the change affects project-wide documentation
2. All referenced documentation files affected by the change (see sections below)
3. All files referenced by those files (recursive update through the entire reference chain)
4. All service-level `AGENTS.md` files in each microservice directory (e.g., `Auth/AGENTS.md`, `Items/AGENTS.md`, etc.)

**Documentation must always stay in sync. Propagate updates through the entire documentation tree.**

---

## Maven usage
When using Maven commands you MUST use the Maven wrapper (`./mvnw`) inside the service you are working on — never from a parent or sibling directory. Always run from the target service's root folder (e.g., `Items/`, `Auth/`).

## File Operations
- **Delete files** using the Write tool (write empty content) or Edit tool — never use bash `rm`.
- **Create/rename directories** using the Write tool with the target file path — never use `mkdir` or `mv`.
- **NEVER run `rm -rf` or `rm -f`** on ANY project-related path. If you accidentally create a file at the wrong path, use the Write tool to clean it up, or ask the user.
- When writing new files, **double-check the file path** — confirm it's under the correct project root before saving.

## Before Committing
**ALWAYS run tests first** — see [docs/TESTING_STRATEGY.md](./docs/TESTING_STRATEGY.md) for which tests to run. Never commit without passing tests.
1. Run unit + integration tests for the affected service from its directory: `./mvnw clean test`
2. If available, also run E2E tests from `e2e-tests/`: `./mvnw clean test`
3. Only commit if ALL tests pass.

## Quick Reference

See [docs/PROJECTS_OVERVIEW.md](./docs/PROJECTS_OVERVIEW.md) (always read that file!) for:
- Main idea of the project
- Services & Ports table

## Maven Build Dependencies & Parallel Builds

### Dependency Graph
Auth - no dependencies on other projects
api-gateway - no dependencies on other projects
Items - depends on `common` (uses shared models/utilities)
common - no dependencies on other projects
e2e-tests - not part of the build graph (contains only tests, build separately when needed)
frontend - not part of the build graph (separate React app, build separately when needed)

### Parallel Build Strategy for agents

When asked to build multiple projects, **analyze the dependency graph above** and run builds in parallel whenever possible to save time.

## Git Workflow

See [docs/GIT_WORKFLOW.md](./docs/GIT_WORKFLOW.md) for:
- Branch naming conventions
- Commit message format (Conventional Commits)
- Pull request guidelines
- Release process
- Versioning strategy (Semantic Versioning)
- Merge policies
- Tagging conventions
- Hotfix procedures
- CI/CD integration
- Code review checklist
- Issue tracking and linking
- Documentation updates
- Rollback procedures
- Changelog maintenance
- Feature branching strategy

## Architecture & API Design

See [docs/API_DESIGN.md](./docs/API_DESIGN.md) for:
- API versioning and request/response format
- Error handling (RFC 9457 Problem Details)
- Observability and metrics standards (tag-based dimensional metrics)
- Logging standards
- Gateway exception: public, unversioned info endpoints (e.g., `/api/product-info`) when no service owns the data

## Style Guides (Condensed)

### Java (Summary)
- Formatting: 4 spaces, 120-char line limit, continuation indent 8 spaces; K&R braces; one blank line between methods and logical sections.
- Modern Java: use `var` only when type is obvious; prefer records for DTOs/value objects; use enhanced switch, pattern matching, sealed types, and text blocks where they improve clarity.
- Naming: packages lowercase; classes/interfaces PascalCase; methods/variables camelCase; constants SCREAMING_SNAKE_CASE; avoid abbreviations.
- Class layout: static fields → instance fields → constructors → public methods → package-private → protected → private.
- Comments: explain why/corner cases; avoid restating obvious code; use Javadoc for public APIs when needed.
- Exceptions: throw specific exceptions; do not catch generic `Exception`.
- Spring: constructor injection required; use Lombok judiciously (prefer records for immutable data).
- Streams/Optionals: use streams for transformations only; prefer immutable collections; no Optional fields/params; avoid `isPresent()+get()`.

### TypeScript/React (Summary)
- TypeScript: strict mode required; avoid `any` (use `unknown` + narrowing or generics).
- Naming: components PascalCase; hooks `use*` camelCase; files match component/hook names; constants SCREAMING_SNAKE_CASE.
- Components: functional only; props interface above component; component order = props → hooks → derived state → handlers → JSX.
- Hooks: include all dependencies in `useEffect`; avoid `useEffect` for derived state; custom hooks return objects, not arrays.
- State/server data: use Zustand for client state; React Query for server state.
- Types: `interface` for object shapes, `type` for unions/intersections; use discriminated unions for request states.
- JSX: avoid index keys; use explicit boolean checks; keep event handlers clear (inline simple, named for complex).
- Styling (Tailwind): order classes by layout → sizing → typography → colors → effects → states; use `cn()` for conditional classes.

## Testing Strategy

See [docs/TESTING_STRATEGY.md](./docs/TESTING_STRATEGY.md) for:
- When to run tests
- Testing levels (unit, integration, e2e)
- Coverage requirements
- Test data management
- Mocking and stubbing guidelines
- CI testing integration
- Performance testing
- Security testing
- Test documentation

## Debug Info

See [docs/DEBUG_INFO.md](./docs/DEBUG_INFO.md) for:
- Troubleshooting guides
- Common issues and solutions

## Future Ideas

See [docs/CONCEPTS_TO_TRY.md](./docs/CONCEPTS_TO_TRY.md) for:
- Experimental concepts to explore (which are the target for future spikes)
- Future improvements

## Planning

- Add all plans in [planning](./planning/) folder
- Use the following name pattern `<feature-name>-PLAN.md`, for example `Migrating-auth-service-to-ddd-PLAN.md`
- In each plan create tasks to be done and when done put ticks on them, so I know what is implemented, what has left, etc.
- Create list with issues also - mainly technological (closed ports, things to be set up, etc). For the issues which are solved put green tick on them and explain how they are fixed briefly. In that way I will know what are the issues which left after the implementation

## Explanations
- When I ask you to save me explanation in a file, you MUST save it in [explanations](./explanations/) folder

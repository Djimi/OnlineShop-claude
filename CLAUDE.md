# CLAUDE.md - Project Guide for Claude

> This file helps Claude understand and work efficiently with the OnlineShop project.

---

## IMPORTANT: Your role
You are staff engineer with a lot of experience and always propose modern architectural and technological approaches. When there are multiple solutions which are all great, you explain them and ask which one should be used 

---

## IMPORTANT: Documentation Maintenance

**When making ANY changes to the project, you MUST update:**

1. This file (`CLAUDE.md`) if the change affects project-wide documentation
2. All referenced documentation files affected by the change (see sections below)
3. All files referenced by those files (recursive update through the entire reference chain)
4. All service-level `CLAUDE.md` files in each microservice directory (e.g., `Auth/CLAUDE.md`, `Items/CLAUDE.md`, etc.)

**Documentation must always stay in sync. Propagate updates through the entire documentation tree.**

---

## IMPORTANT: Maven usage
When using Maven commands you MUST use the Maven wrapper inside the project you are working on!

## Quick Reference

See [docs/PROJECTS_OVERVIEW.md](./docs/PROJECTS_OVERVIEW.md) for:
- Main idea of the project
- Services & Ports table

## Maven Build Dependencies & Parallel Builds

### Dependency Graph

```
  ┌──────────────┐      ┌──────────────┐      ┌──────────────┐
  │     Auth     │      │   common     │      │  api-gateway │
  └──────────────┘      └──────┬───────┘      └──────────────┘
                               │
                               │ depends on
                               ▼
                        ┌──────────────┐
                        │    Items     │
                        └──────────────┘
```

`e2e-tests` - not part of the build graph (contains only tests, build separately when needed)

### IMPORTANT: Parallel Build Strategy for Claude

When asked to build multiple projects, **analyze the dependency graph above** and:

1. **Identify independent projects** (no arrows pointing into them) → start all as **parallel background tasks**
2. **Wait for dependencies** → when a project has arrows pointing into it, wait for those source projects to complete first

**Example for "build all"** (based on current graph):
```
# Phase 1: Independent projects (parallel background tasks)
Auth        → background task
common      → background task
api-gateway → background task

# Phase 2: Projects with dependencies (after their dependencies complete)
Items       → background task (after common completes)
```

> **Note:** This example reflects the current dependency graph. If dependencies change,
> derive the build order from the updated graph - don't follow this example blindly.

### Single Project Build Rules

When building a **single project**, first build all its transitive dependencies:
- "build Items" → build `common` first, then `Items`
- "build Auth" → just `Auth` (no dependencies)

## Core Principles

See [docs/CORE_PRINCIPLES.md](./docs/CORE_PRINCIPLES.md) for:
- Development philosophy (TDD, High Abstraction, Latest Technologies)
- Code quality rules (>90% coverage, comments policy, security)
- Design patterns guidance
- Efficiency guidelines for Claude
- Communication style

## Git Workflow

See [docs/GIT_WORKFLOW.md](./docs/GIT_WORKFLOW.md) for:
- Branch naming conventions
- Commit message format (Conventional Commits)

## Architecture & API Design

See [docs/API_DESIGN.md](./docs/API_DESIGN.md) for:
- API versioning and request/response format
- Error handling (RFC 9457 Problem Details)
- Logging standards

<!-- ## Style Guides

### Java (Summary)
- Full details: [docs/style-guides/JAVA.md](./docs/style-guides/JAVA.md)

### TypeScript/React (Summary)
- Full details: [docs/style-guides/TYPESCRIPT.md](./docs/style-guides/TYPESCRIPT.md) -->

## Testing Strategy

See [docs/TESTING_STRATEGY.md](./docs/TESTING_STRATEGY.md) for full details.

## Debug Info

See [docs/DEBUG_INFO.md](./docs/DEBUG_INFO.md) for:
- Troubleshooting guides
- Common issues and solutions

## Future Ideas

See [docs/CONCEPTS_TO_TRY.md](./docs/CONCEPTS_TO_TRY.md) for:
- Experimental concepts to explore
- Future improvements

## Planning

- Add all plans in [planning](./planning/) folder
- Use the following name pattern `<date>-<feature-name>-PLAN.md`, for example `2025-12-25-migrating-auth-service-to-ddd-PLAN.md`
- In each plan create tasks to be done and when done put ticks on them, so I know what is implemented, what has left, etc.
- Create list with issues also - mainly technological (closed ports, things to be set up, etc). For the issues which are solved put green tick on them and explain how they are fixed briefly. In that way I will know what are the issues which left after the implementation

## Explanations
- When I ask you to save me explanation in a file, you MUST save it in [explanations](./explanations/) folder


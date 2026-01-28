---
name: java-dependency-upgrader
description: Upgrade Java/Maven/Spring dependency and plugin versions across multi-module or multi-project Maven repositories. Use when asked to "upgrade all Java dependencies" or modernize dependency versions while enforcing Spring Boot/Cloud BOM usage, avoiding duplicates, running builds/tests, and validating the full docker-compose + e2e flow.
---

# Java Dependency Upgrader

## Overview
Upgrade all Maven dependency and plugin versions across immediate subprojects with a single, deduplicated version-check list; enforce Spring Boot/Cloud BOM usage; run `./mvnw verify` per project; then cycle docker compose and run e2e tests.

## Workflow (sequential)

### 1) Discover projects
- Start at repo root.
- List only first-level subfolders.
- Treat a folder as a Maven project when it contains `pom.xml`.
- Keep a stable list of project paths and use those throughout the run.

### 2) Build a single dependency + plugin inventory
- Parse all `pom.xml` files (and relevant parent POMs) to collect:
  - Dependencies (groupId, artifactId, version or version property).
  - Plugins (groupId, artifactId, version or version property).
  - BOM imports (dependencyManagement with `<type>pom</type>` + `<scope>import</scope>`).
  - Java/Maven/Spring-related version properties (e.g., `java.version`, `maven.compiler.release`, `spring-boot.version`, `spring-cloud.version`).
- Normalize into a single deduplicated list keyed by `groupId:artifactId` plus a list of property keys that define versions.
- Keep a mapping of where each item appears to apply consistent updates later.

### 3) Identify upgrade targets (stable-only, official repos)
- Check official repositories only (Maven Central, Spring repositories when applicable) for the latest *stable* releases.
- Include plugin versions in the same pass.
- Do not query the same dependency multiple times; use the deduplicated list.

### 4) Apply versioning rules (Spring + Maven constraints)
- **Spring Boot:** use the Spring Boot Maven plugin and set only its version (prefer a single `spring-boot.version` property if present). Manage Spring dependencies via the Spring Boot BOM; remove explicit versions for dependencies covered by the BOM.
- **Spring Cloud:** set only the Spring Cloud BOM version (prefer a single `spring-cloud.version` property). Manage Spring Cloud dependencies via the BOM; avoid explicit versions covered by the BOM.
- **No downgrades:** never move to an older version to fix failures. If compatibility breaks, fix code/config or keep the newer version and iterate.
- **Plugins:** update plugin versions similarly, preferring shared properties when used.
- **Java/Maven:** update Java version and Maven-related settings consistently (e.g., `maven.compiler.release`, `java.version`, toolchains if present). Keep them aligned across projects.

### 5) Update POMs consistently
- Apply updates across all affected POMs using the global inventory so that shared dependencies resolve to the same version where appropriate.
- If a dependency is under a BOM, remove explicit version fields for that dependency.
- Keep version properties in one place (root or parent) when possible; avoid scattering duplicate version properties.

### 6) Verify builds and tests per project
- For each Maven project, run `./mvnw verify` from that project directory.
- Use Maven Wrapper *inside* each project.
- If any build fails, iterate on fixes (dependency updates, configuration changes, code adjustments) without downgrading versions.

### 7) Recreate docker compose stack and run e2e
- Follow `docs/DEBUG_INFO.md` for the exact docker compose and e2e commands.
- Stop the full stack and remove volumes, then bring it up again and run e2e tests.
- If any step fails, iterate and re-run until green, without downgrading versions.

## Decision rules
- Use stable releases only (no snapshots, milestones, betas, RCs).
- Prefer aligning versions across projects unless there is a strong reason not to.
- Avoid repeating checks for the same dependency by using the shared inventory.
- If multiple upgrade paths are viable, explain options and ask which to use.

## Notes
- If a project inherits from a parent POM within the repo, update version properties at the highest common parent to keep consistency.
- When unsure whether a dependency is managed by a BOM, verify the BOM contents before removing explicit versions.
- For container and e2e instructions, always open `docs/DEBUG_INFO.md` instead of copying its content.

# Claude Code Workflow Optimization Plan

## Executive Summary

After analyzing your OnlineShop project and global Claude settings, I've identified key optimization opportunities. Your **global settings are already excellent** but the **project-level settings override them** with restrictive permissions.

**Current State:**
- ✅ Global permissions comprehensive (`git:*`, `docker:*`, `./mvnw.cmd:*`, `npm:*`, etc.)
- ⚠️ Project `.claude/settings.local.json` is too restrictive (only `mvn test:*`, `xargs ls:*`)
- ⚠️ Syntax error in global settings.json (missing comma line 26)
- ❌ No custom commands or skills defined
- ❌ No .claudeignore file
- ✅ Good documentation structure

---

## 1. Fix Global Settings Syntax Error

### File: `C:\Users\DamyanManev\.claude\settings.json`

**Issue:** Missing comma after line 26

```json
// Current (broken):
"Bash(xargs:*)"
"Bash(gh issue view:*)",

// Fixed:
"Bash(xargs:*)",
"Bash(gh issue view:*)",
```

---

## 2. Update Project Permissions (or Remove Override)

### Option A: Remove project-level override (RECOMMENDED)
**File:** `d:\CodingProjects\OnlineShop-claude\.claude\settings.local.json`

Simply delete or empty this file to inherit your comprehensive global permissions.

### Option B: Add project-specific extensions
If you want project-specific additions, keep only what's extra:
```json
{
  "permissions": {
    "allow": [
      "Bash(mvn:*)"
    ]
  }
}
```

**Note:** Project settings ADD to global settings, they don't replace them. Your current restrictive project settings are limiting you unnecessarily.

---

## 3. Create Custom Slash Commands

Create commands in `C:\Users\DamyanManev\.claude\commands\` (global) or `d:\CodingProjects\OnlineShop-claude\.claude\commands\` (project-specific):

### `/build-all` - Build All Services
**File:** `.claude/commands/build-all.md`
```markdown
Build all microservices respecting the Maven dependency graph:

**Phase 1 (Parallel - No dependencies):**
- Auth service
- common module
- api-gateway

**Phase 2 (After common completes):**
- Items service (depends on common)

Use background tasks for Phase 1, wait for common to complete, then build Items.
Report build status and any failures.
```

### `/test-service` - Test Specific Service
**File:** `.claude/commands/test-service.md`
```markdown
Run tests for the specified service: $ARGUMENTS

1. Navigate to the service directory
2. Run: ./mvnw.cmd clean test jacoco:report
3. Parse and report:
   - Test results (passed/failed/skipped)
   - Code coverage percentage from JaCoCo
   - Any test failures with details
4. If coverage < 90%, warn about coverage gap
```

### `/check-coverage` - Coverage Report
**File:** `.claude/commands/check-coverage.md`
```markdown
Check JaCoCo coverage for service: $ARGUMENTS

1. If no argument, check all services
2. Read target/site/jacoco/index.html for each service
3. Report coverage by package
4. Highlight any packages below 90% threshold
5. Suggest which areas need more tests
```

### `/docker-status` - Docker Environment Status
**File:** `.claude/commands/docker-status.md`
```markdown
Check Docker environment status:

1. Run docker-compose ps to show container status
2. Check health of each service
3. Report any unhealthy or stopped containers
4. Show recent logs for any failing services
```

### `/create-plan` - Create New Plan File
**File:** `.claude/commands/create-plan.md`
```markdown
Create a new plan file for feature: $ARGUMENTS

1. Generate filename: planning/YYYY-MM-DD-<feature-name>-PLAN.md
2. Use this template:

# <Feature Name> Plan

## Overview
<Brief description>

## Tasks

| Task | Status | Notes |
|------|--------|-------|
| Task 1 | PENDING | |
| Task 2 | PENDING | |

## Implementation Checklist
- [ ] Write failing tests (TDD)
- [ ] Implement feature
- [ ] Update documentation
- [ ] Run full test suite
- [ ] Check coverage > 90%

## Issues & Blockers

| Issue | Status | Solution |
|-------|--------|----------|

## Notes
```

### `/update-docs` - Update Documentation Chain
**File:** `.claude/commands/update-docs.md`
```markdown
After making changes, update the documentation chain:

1. Identify what changed (files, APIs, configurations)
2. Update root CLAUDE.md if needed
3. Update relevant docs/ files:
   - API_DESIGN.md for API changes
   - TESTING_STRATEGY.md for test changes
   - DEBUG_INFO.md for new troubleshooting info
4. Update service-level CLAUDE.md files
5. Report all files updated
```

---

## 4. Custom Skills (Already Have Two)

You already have these skills configured:
- `/github-fix-issue` - Analyze and fix GitHub issues
- `/find-newest-dependency-versions` - Check for dependency updates

**Optional New Skill:** `run-integration-tests.md`
```markdown
---
name: run-integration-tests
description: Run integration tests with Testcontainers for a service
---
Run integration tests for service: $ARGUMENTS
1. Verify Docker is running
2. Execute: ./mvnw.cmd test -Dtest=*IntegrationTest
3. Report results and Testcontainers errors
```

---

## 5. Optimize Token Usage

### Create `.claudeignore`

**File:** `.claudeignore`
```
# Build outputs
**/target/
**/build/
**/dist/
**/node_modules/

# IDE files
.idea/
*.iml
.vscode/extensions/

# Logs and temp files
*.log
*.tmp
*.bak
nul

# Large generated files
**/jacoco-report/
**/surefire-reports/
**/*.jar
**/*.war

# Git internals
.git/objects/
.git/logs/

# Dependencies lock files (large)
package-lock.json
pom.xml.versionsBackup

# Test artifacts
**/test-output/
**/__pycache__/

# Docker volumes
**/data/
```

**Impact:** Reduces tokens by excluding large irrelevant directories from searches.

### Documentation Reference Pattern

Instead of duplicating content, use references:
```markdown
## Testing
See [docs/TESTING_STRATEGY.md](./docs/TESTING_STRATEGY.md) for complete details.
```

You're already doing this well in CLAUDE.md - maintain this pattern.

---

## 6. Add Hooks for Automation (Optional)

### Pre-Edit Hook (Optional)
**File:** `.claude/hooks/pre-edit.sh`
```bash
#!/bin/bash
# Backup file before edit
cp "$1" "$1.bak" 2>/dev/null || true
```

### Post-Test Hook Suggestion
Configure in settings to auto-run tests after code changes:

```json
{
  "hooks": {
    "postToolUse": {
      "Edit": "echo 'File edited: consider running tests'"
    }
  }
}
```

---

## 7. Improve CLAUDE.md Instructions

### Add Quick Commands Section

Add to root CLAUDE.md:
```markdown
## Quick Commands for Claude

When working with this project, these commands are pre-approved:
- `./mvnw.cmd clean test` - Run tests
- `./mvnw.cmd jacoco:report` - Generate coverage
- `docker-compose ps` - Check services
- `git status/log/diff` - Git inspection

### Common Tasks
1. **Build a service:** `/build-all` or `cd <service> && ./mvnw.cmd clean package`
2. **Run tests:** `/test-service <service-name>`
3. **Check coverage:** `/check-coverage <service-name>`
4. **Create feature plan:** `/create-plan <feature-name>`
```

### Add Service Quick Reference Table

```markdown
## Service Quick Reference

| Service | Directory | Build | Test | Port |
|---------|-----------|-------|------|------|
| Auth | `/Auth` | `./mvnw.cmd clean package` | `./mvnw.cmd test` | 9001 |
| Items | `/Items` | `./mvnw.cmd clean package` | `./mvnw.cmd test` | 9000 |
| Gateway | `/api-gateway` | `./mvnw.cmd clean package` | `./mvnw.cmd test` | 10000 |
| Common | `/common` | `./mvnw.cmd clean install` | `./mvnw.cmd test` | N/A |
```

---

## 8. Additional Optimizations

### A. Create Service-Level CLAUDE.md Files

Missing for: `common/`, `api-gateway/`, `frontend/`

Template:
```markdown
# <Service> Service - Claude Guide

## Quick Reference
- **Port:** <port>
- **Tech:** <technologies>
- **Directory:** `/<service>/`

## Common Commands
- Build: `./mvnw.cmd clean package`
- Test: `./mvnw.cmd test`
- Coverage: `./mvnw.cmd jacoco:report`

## Project Structure
<brief structure>

## Key Files
- Configuration: `src/main/resources/application.yml`
- Main class: `src/main/java/.../Application.java`

See [root CLAUDE.md](../CLAUDE.md) for project-wide guidelines.
```

### B. Use Explore Agents for Discovery

When you need to understand code, explicitly ask:
> "Use the Explore agent to find how authentication works"

This uses specialized agents optimized for codebase exploration.

### C. Parallel Agent Usage

For complex tasks, request parallel execution:
> "Run tests for Auth and Items services in parallel"

Claude will use multiple background tasks.

---

## Implementation Checklist

Based on your preferences (full expansion, all commands, all CLAUDE.md files):

### Phase 1: Fix Settings (Quick Wins)
- [ ] Fix syntax error in `C:\Users\DamyanManev\.claude\settings.json` (add comma line 26)
- [ ] Remove/simplify `d:\CodingProjects\OnlineShop-claude\.claude\settings.local.json`

### Phase 2: Create Slash Commands
- [ ] Create `C:\Users\DamyanManev\.claude\commands\` directory
- [ ] Create `/build-all.md` command
- [ ] Create `/test-service.md` command
- [ ] Create `/check-coverage.md` command
- [ ] Create `/create-plan.md` command
- [ ] Create `/update-docs.md` command

### Phase 3: Token Optimization
- [ ] Create `d:\CodingProjects\OnlineShop-claude\.claudeignore`

### Phase 4: Documentation
- [ ] Create `common/CLAUDE.md`
- [ ] Create `api-gateway/CLAUDE.md`
- [ ] Create `frontend/CLAUDE.md`
- [ ] Update root `CLAUDE.md` with Quick Commands section

### Phase 5: Optional Enhancements
- [ ] Configure hooks (if desired)
- [ ] Add new skills

---

## Expected Benefits

| Optimization | Benefit |
|--------------|---------|
| Fix settings | Unlock all your global permissions for this project |
| .claudeignore | ~40% token reduction in file searches |
| Slash commands | Consistent, repeatable workflows (type `/build-all` instead of explaining) |
| Service CLAUDE.md | Faster context for service-specific work |

---

## Files to Create/Modify

| File | Action |
|------|--------|
| `C:\Users\DamyanManev\.claude\settings.json` | Fix comma syntax error |
| `d:\CodingProjects\OnlineShop-claude\.claude\settings.local.json` | Simplify or delete |
| `C:\Users\DamyanManev\.claude\commands\build-all.md` | Create |
| `C:\Users\DamyanManev\.claude\commands\test-service.md` | Create |
| `C:\Users\DamyanManev\.claude\commands\check-coverage.md` | Create |
| `C:\Users\DamyanManev\.claude\commands\create-plan.md` | Create |
| `C:\Users\DamyanManev\.claude\commands\update-docs.md` | Create |
| `d:\CodingProjects\OnlineShop-claude\.claudeignore` | Create |
| `d:\CodingProjects\OnlineShop-claude\common\CLAUDE.md` | Create |
| `d:\CodingProjects\OnlineShop-claude\api-gateway\CLAUDE.md` | Create |
| `d:\CodingProjects\OnlineShop-claude\frontend\CLAUDE.md` | Create |
| `d:\CodingProjects\OnlineShop-claude\CLAUDE.md` | Update with Quick Commands |

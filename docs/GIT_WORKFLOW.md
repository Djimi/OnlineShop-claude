# Git Workflow

## Branch Naming

- Features: `feature/<description>` (e.g., `feature/add-cart-service`)
- Bug fixes: `bug/<description>` (e.g., `bug/fix-auth-token-refresh`)
- **Never commit directly to main**

## Commit Message Format (Conventional Commits)

```
<type>(<scope>): <description>

[optional body]

Types: feat, fix, refactor, test, docs, chore, perf
Scopes: auth, items, gateway, frontend, e2e, docs
```

### Examples

```
feat(auth): add password reset endpoint
fix(items): correct price calculation for bulk discounts
test(e2e): add checkout flow scenarios
```

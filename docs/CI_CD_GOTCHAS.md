# CI/CD Gotchas — Quick Reference

> Read before working on any CI/CD or AWS infra task. Condensed from actual debugging runs (see [plans/AUTOMATIC-BUILDS-AND-DEPLOY/AWS_COMMANDS_GUIDE.md](../plans/AUTOMATIC-BUILDS-AND-DEPLOY/AWS_COMMANDS_GUIDE.md) for the full narrative).

---

## Pre-flight Checks

1. **Identify yourself:** `aws sts get-caller-identity` — always first in any terminal.
2. **Confirm region:** `aws configure set region eu-north-1` or pass `--region eu-north-1` to every command.
3. **Check existing state:** `aws ecr describe-repositories --region eu-north-1`, `aws iam list-roles --query "..."` before creating anything.

---

## AWS Context

| Property | Value |
|----------|-------|
| Account ID | `799111666795` |
| Region | `eu-north-1` |
| OIDC Provider | `arn:aws:iam::799111666795:oidc-provider/token.actions.githubusercontent.com` |
| IAM Role | `arn:aws:iam::799111666795:role/github-actions-onlineshop` |
| ECR Registry | `799111666795.dkr.ecr.eu-north-1.amazonaws.com` |
| ECR Naming | `onlineshop-<service>` (NO SLASHES — e.g. `onlineshop-auth`, not `onlineshop-auth/api-gateway`) |

---

## GitHub Actions Workflows

| Gotcha | Why It Happens | Rule |
|--------|---------------|------|
| `workflow_dispatch` invisible on feature branch | GitHub only indexes from the default branch (`main`) | During development, temporarily add `push` trigger. Remove before merging. |
| `github.event.inputs` is `null` on push events | `.inputs` only exists for `workflow_dispatch` events | Guard with `github.event_name == 'workflow_dispatch'` before accessing `.inputs`. Use `github.event_name == 'push'` as a catch-all during development. |
| `Cache export is not supported for the docker driver` | `cache-from: type=gha` requires BuildKit, but the runner's default Docker driver doesn't support it | Always add `docker/setup-buildx-action@v3` before any `docker/build-push-action` that uses `cache-from`/`cache-to`. |
| Java version mismatch: `release version X not supported` | `java-version` in `setup-java` doesn't match `<java.version>` in `pom.xml` or the `FROM` image in `Dockerfile` | Cross-check all three sources of truth before setting the version in the workflow. |
| `Could not find or load main class ...MavenWrapperMain` | `maven-wrapper.jar` was tracked in git and got corrupted by CRLF normalisation | `maven-wrapper.jar` is in `.gitignore` and auto-downloaded. Never track it. |
| Jobs all "skipped" on push | Job `if:` condition only checked `github.event.inputs.service` which is `null` on push | Always include `github.event_name == 'push'` as an OR condition in job guards during development. |

---

## AWS CLI on Windows

PowerShell's `@'...'@` here-strings write UTF-8 with a Byte Order Mark (BOM). AWS IAM (and many other AWS services) reject JSON with a BOM because they expect pure ASCII.

**Wrong:**
```powershell
$json = @'
{"Version":"2012-10-17",...}
'@
$json | Out-File -FilePath trust-policy.json -Encoding utf8
```

**Right:**
```powershell
$json = '{"Version":"2012-10-17",...}'
[System.IO.File]::WriteAllText("trust-policy.json", $json, [System.Text.Encoding]::ASCII)
```

---

## Git Binary Safety

- **Auto-downloadable binaries belong in `.gitignore`, never in git tracking** (e.g., `maven-wrapper.jar`, `node_modules`). They can be corrupted by git's line-ending conversion if accidentally tracked.

---

## AWS ECR

- ECR repositories are **region-scoped** — repos in `eu-north-1` are invisible in `eu-central-1`
- `delete-repository --force` is destructive and irreversible. Always run `describe-images` first to confirm the repo is empty (or you're okay losing the images).
- Use `aws ecr describe-images --repository-name <name> --region eu-north-1 --query "imageDetails[*].imageTags[0]"` to verify pushed images

---

## Verification Pattern

Every mutating AWS command should be immediately verified:

| Mutation | Verification |
|----------|-------------|
| `create-role` | `get-role --role-name <name>` |
| `put-role-policy` | `list-role-policies --role-name <name>` |
| `create-repository` | `describe-repositories --repository-name <name> --region <region>` |
| `delete-repository` | `describe-repositories --region <region>` (confirm it's gone) |

AWS CLI returns empty output on success for many commands — silence does NOT mean it worked. Verify explicitly.

# Pass 2 — CI Pipeline Hardening & Staging

**Goal:** Transform the manual MVP into an automated CI/CD pipeline with proper test gates, selective builds, branch protection, and a staging environment.

**Prerequisite:** Pass 1 complete (services running on AWS).

**Exit criteria:** Pushing to a feature branch triggers automated build+test for affected services. PRs to `main` are gated by tests. A staging environment exists for pre-production validation.

---

## Tasks

### 2.1 Branch Protection & Merge Policy

- [ ] Enable `main` branch protection:
  - Require passing status checks
  - Require at least one approval
  - Dismiss stale approvals on new pushes
  - Require linear history (squash merge only)
- [ ] Forbid direct pushes to `main`
- [ ] Configure concurrency groups so older builds on the same branch are canceled when new commits arrive

### 2.2 Automated CI Triggers

- [ ] Trigger CI on pushes to `feature/*` branches
- [ ] Trigger CI on pull requests targeting `main`
- [ ] Keep `workflow_dispatch` for manual runs

### 2.3 Change Detection & Selective Builds

- [ ] Implement automated change detection (e.g., `dorny/paths-filter` or custom script analyzing `git diff`)
- [ ] Define dependency graph in the workflow:
  - `common` changes → rebuild `common` + `Items`
  - `Auth` changes → rebuild `Auth` only
  - `api-gateway` changes → rebuild `api-gateway` only
  - `frontend` changes → rebuild `frontend` only
  - Root-level / shared config changes → rebuild all affected services
- [ ] Ensure `common` + `Items` are validated against the same repository snapshot (req 01 §5.2)
- [ ] No full-repository rebuild as default fallback — dependency-aware selective rebuild only

### 2.4 Test Gates

- [ ] **Feature-branch pushes:** run unit + integration tests for affected services
  - Target: under 5 minutes; hard limit: 10 minutes
- [ ] **PRs to `main`:** run unit + integration + frontend validation for affected components
- [ ] **Auth:** enforce 50% minimum coverage gate (JaCoCo — already configured)
- [ ] **E2E gate on PR:** blocking e2e validation must pass before merge
  - Run against merge-queue candidate or equivalent pre-merge integration candidate
  - Does NOT need to run on every feature-branch push

### 2.5 Docker Tagging Model

- [ ] Every image gets an immutable tag: `sha-<FULL_OR_SHORT_SHA>`
- [ ] Feature-branch images: SHA tag + optional `branch-<name>` convenience tag
- [ ] `main` candidate images: SHA tag + mutable `main-latest` convenience pointer
- [ ] Feature-branch and `main`-candidate images are non-official artifacts
- [ ] Push all images to ECR on successful build

### 2.6 Caching Optimization

- [ ] Verify GitHub Actions Maven dependency cache is working (hit rate in logs)
- [ ] Verify Docker layer cache is working
- [ ] Confirm cache miss does not break builds (correctness requirement — req 01 §7.4)

### 2.7 Staging Environment

- [ ] Create a staging ECS cluster (or reuse cluster with separate services/namespace)
- [ ] Staging should be **on-demand** (prefer short-lived to reduce cost — req 02 §3.2):
  - Option A: Scale staging services to 0 when idle, scale up on deploy
  - Option B: Spin up staging via IaC on demand, tear down after validation
- [ ] Deploy `main` candidate images to staging automatically after successful `main` build
- [ ] Run e2e tests against staging after deployment
- [ ] Staging database: lightweight instance or shared with seeded test data

### 2.8 CI Security

- [ ] Confirm OIDC is the only auth mechanism for GitHub → AWS (no long-lived keys)
- [ ] Confirm secrets are fetched from AWS Secrets Manager at runtime
- [ ] Verify secret masking in GitHub Actions logs

---

## Cost Impact

| Addition | Estimated Cost |
|---|---|
| GitHub Actions minutes (Free Tier: 2000 min/month) | $0 (within limits) |
| Staging ECS (on-demand, short-lived) | ~$0.50–1.00 |
| Additional ECR images (feature branches) | ~$0.10 |
| **Incremental total** | **~$0.50–1.00** |

---

## Out of Scope for Pass 2

- Official release promotion flow (Pass 3)
- Release manifests, SBOM, provenance (Pass 3)
- ECR lifecycle / retention policies (Pass 3)
- Rollback mechanism (Pass 3)
- Notifications (Pass 4)
- Dashboards & runbooks (Pass 4)
- Nightly full validation (Pass 4)

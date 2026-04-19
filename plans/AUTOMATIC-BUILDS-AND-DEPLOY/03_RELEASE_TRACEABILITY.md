# Pass 3 — Release, Traceability & Promotion

**Goal:** Establish the official release identity model, deployment promotion pipeline (staging → production), rollback capability, and full commit-to-deployment traceability.

**Prerequisite:** Pass 2 complete (automated CI with test gates, staging environment exists).

**Exit criteria:** Official releases can be promoted through staging to production with manual approval. Every deployed artifact is traceable back to its exact commit. Rollback to a previous release is a documented, safe operation.

---

## Tasks

### 3.1 Release Identity Model

- [ ] No manual version numbers in source code — release identity derived from git
- [ ] Primary source of truth: monorepo commit SHA
- [ ] User-friendly release label (e.g., `1.2.1`) generated during the release process:
  - Option A: Conventional-commits-based auto-versioning (e.g., `semantic-release` or `release-please`)
  - Option B: Manual label assignment during promotion (simpler for v1)
- [ ] Release labels are component-scoped: `auth/1.0.0`, `items/1.2.1`, etc.
- [ ] Internal shared libraries (`common`) follow the same SHA model — no standalone version bumps

### 3.2 Official Release Promotion Flow

- [ ] Define "promote to release" as an explicit action (GitHub Actions workflow triggered manually or by GitHub Release creation)
- [ ] Promotion flow:
  1. Select a `main` candidate build (by SHA or build ID)
  2. Generate/assign user-friendly release label
  3. Re-tag Docker image with official release tag (e.g., `auth:1.0.0` + `auth:sha-abc123`)
  4. Generate release manifest (machine-readable JSON/YAML)
  5. Generate SBOM / provenance artifact (e.g., via `syft`, `cosign`, or Docker buildx provenance)
  6. Create GitHub Release with manifest + SBOM attached
- [ ] Only intentional promotions produce official releases — successful `main` builds are candidates, not releases

### 3.3 Docker Tagging for Official Releases

- [ ] Official release images get: immutable SHA tag + user-friendly release tag (e.g., `1.0.0`)
- [ ] If a global `latest` tag exists, it must point to the latest official release (not the latest `main` build)
- [ ] Deployment must always resolve to immutable tag or digest — never to a mutable tag alone

### 3.4 Deployment Promotion: Staging → Production

- [ ] Deploy release candidate to staging automatically (or on demand)
- [ ] Run e2e validation against staging
- [ ] After staging validation passes → request production approval
- [ ] **Production approval:** manual, owner-only (req 02 §5.3)
- [ ] Deploy approved release to production ECS services
- [ ] Record: who approved, what artifact, when

### 3.5 Production Environment

- [ ] Create production ECS services (may share the same cluster as staging, different services/task definitions)
- [ ] Production database: RDS instance (separate from staging)
- [ ] Production security groups: tighten as needed
- [ ] Production task definitions reference immutable image tags/digests

### 3.6 Rollback

- [ ] Rollback targets a previously known-good immutable artifact (req 02 §6)
- [ ] Rollback requires manual approval (owner-only)
- [ ] The system must make it easy to identify the previous known-good release:
  - List recent official releases with their SHA + label
  - One-click (or one-command) redeploy of a selected release
- [ ] Rollback window: most recent 10 official release images in ECR

### 3.7 Traceability Chain

- [ ] Every build retains metadata answering (req 01 §10.2):
  - Which commit SHA produced this image?
  - Which CI run produced it?
  - Which branch or merge candidate was validated?
  - Which component release identity was assigned?
  - Which `common` revision was included (for Items)?
  - Is this a branch build, `main` candidate, or official release?
- [ ] Images carry OCI labels with traceability metadata (e.g., `org.opencontainers.image.revision`, `org.opencontainers.image.source`)
- [ ] From a running deployment → identify the exact artifact (image digest query on ECS task)
- [ ] From a commit SHA → find the build and image
- [ ] From a release identity → find components, build evidence, manifest, SBOM, image identities

### 3.8 ECR Retention Policies

- [ ] Feature-branch images: expire after ~30 days
- [ ] `main` candidate images: expire after ~30 days (unless referenced by a release)
- [ ] Official release images: keep the most recent 10 per component
- [ ] Official release metadata (manifests, SBOMs, GitHub Releases): retained indefinitely

---

## Cost Impact

| Addition | Estimated Cost |
|---|---|
| Production ECS (3 tasks, Fargate Spot, minimal) | ~$1–3 |
| Production RDS (Free Tier or `db.t3.micro`) | $0–$15 (depends on Free Tier status) |
| ECR storage (10 releases × 4 components) | ~$0.20 |
| **Incremental total** | **~$1–4** |

> **Warning:** Running both staging + production may push total above $5. Mitigate by making staging on-demand (scale to 0 / tear down when idle). If still over budget, staging can share the production RDS with a separate schema.

---

## Out of Scope for Pass 3

- Slack/email notifications (Pass 4)
- Dashboards & cost visibility (Pass 4)
- Runbooks (Pass 4)
- Nightly full validation (Pass 4)
- Merge queue (Pass 4)
- Custom domain / HTTPS (Pass 4, optional)

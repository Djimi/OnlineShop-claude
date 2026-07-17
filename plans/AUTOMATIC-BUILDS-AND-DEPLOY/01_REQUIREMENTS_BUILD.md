# Build And Release Requirements

## 1. Purpose

This document defines the requirements for automated build, test, release, versioning, tagging, and traceability for the OnlineShop repository.

These requirements apply to the current monorepo structure with independently releasable components and internal shared modules:

Independently releasable components:

- Auth
- Items
- API Gateway
- Frontend

some test change here
Internal shared library:

- common

## 2. Scope

The build phase covers:

- branch and pull request validation
- change detection and selective builds
- unit, integration, frontend, and e2e test gates
- Docker image production and tagging
- release versioning
- commit, build, image, and release traceability
- artifact retention
- build notifications and auditability

The build phase does not define production infrastructure implementation details. Those are covered in [02_REQUIREMENTS_DEPLOY.md](./02_REQUIREMENTS_DEPLOY.md) and [03_REQUIREMENTS_HOSTING.md](./03_REQUIREMENTS_HOSTING.md).

## 3. Branch And Merge Policy

### 3.1 Branch model

- The repository uses `feature/*` branches and `main` only for v1.
- Direct pushes to `main` are forbidden.
- All changes to `main` must go through pull requests.

### 3.2 `main` protection

- `main` must require passing status checks.
- `main` must require at least one approval.
- Stale approvals must be dismissed when new commits are pushed.
- Repository history on `main` must remain linear.
- Squash merge is the required merge strategy.

### 3.3 Merge validation behavior

- Validation must run against the effective merge result with the latest `main`, not only against the feature-branch head.
- The preferred mechanism is merge queue or an equivalent pre-merge validation flow that tests the exact candidate to be merged.
- If multiple commits are pushed to the same feature branch in quick succession, older running builds must be canceled and only the latest relevant build may continue.

## 4. Build Trigger Policy

### 4.1 Trigger types

- CI must run on pushes to `feature/*`.
- CI must run on pull requests targeting `main`.
- Full scheduled validation may run nightly across the repository.

### 4.2 Selective build behavior

- The pipeline must build and test only affected services when possible.
- Affected-service detection must include shared and root-level inputs, not only source code under a service directory.
- Affected-service detection must be driven by an automated dependency graph or equivalent automated change-impact analysis rather than by manual version touching or manual trigger selection.

### 4.3 Safe fallback behavior

- When a change affects only one isolated service, only that service and its relevant checks should run.
- When a change affects shared or root-level build inputs, the pipeline must run all directly dependent services that could be impacted by the change.
- A full-repository rebuild is not the default fallback for v1. Dependency-aware selective rebuild is the required behavior.

## 5. Shared Library Policy

### 5.1 Current shared dependency topology

- The `common` module is currently consumed by `Items`.
- Shared-library policy in v1 is defined around this actual dependency.

### 5.2 CI validation rule for `common`

- When `common` changes, CI must rebuild and retest `common` and `Items` against the same repository snapshot before merge.
- This same-snapshot validation exists to detect compatibility issues immediately and must not wait for a separate manual dependency-release cycle.

### 5.3 Release dependency rule for `common`

- Validation and release traceability are separate concerns.
- `Items` must be able to consume `common` changes from the same monorepo snapshot without manual version bumps or separate `common` release steps.
- Official `Items` releases must record the exact monorepo commit SHA, build identity, and resolved internal `common` revision they contain.
- Compatibility between `common` and `Items` must be enforced by dependency-aware CI validation on the same repository snapshot, not by manual internal library version management.

## 6. Test Matrix Requirements

### 6.1 Feature-branch pushes

- On `feature/*` pushes, CI must run unit and integration tests for affected services.
- The target duration for a normal affected-service branch build is under 5 minutes, with 10 minutes treated as the upper acceptable limit.

### 6.2 Pull requests to `main`

- Pull requests to `main` must run unit, integration, and frontend validation for affected components.
- Blocking e2e validation must happen before merge, but it does not need to run on every feature-branch push.
- The pre-merge blocking e2e stage must run on the merge-queue candidate or equivalent pre-merge integration candidate.

### 6.3 Post-merge and nightly validation

- Nightly builds must run all tests for all services.
- Post-merge validation may rerun the full validation set for observability, but release safety must not depend on post-merge discovery of issues that should have blocked merge.

### 6.4 Coverage gates

- A 50% minimum coverage gate applies where coverage reporting is already configured.
- For v1, the enforced coverage gate applies to Auth.
- Other services may adopt coverage gates later, but they are not mandatory for v1 unless coverage reporting is added there.

## 7. CI Platform Requirements

### 7.1 Primary CI system

- GitHub Actions is the primary CI orchestration platform for v1.
- Jenkins is not required for v1.

### 7.2 Workflow structure

- Workflows must remain service-oriented so services can build and evolve independently.
- Repository-level coordination is required for automated dependency-graph-aware change detection and dependency-aware triggering.

### 7.3 Runner policy

- v1 must prioritize the simplest low-overhead runner model consistent with the cost target.
- GitHub-hosted runners are the default starting point for v1 unless a lower-cost option can be demonstrated without materially increasing maintenance burden.

### 7.4 Caching

- GitHub Actions dependency caching for Java and Node dependencies is mandatory for v1.
- Docker layer caching is mandatory for image-producing jobs in v1.
- Build correctness must not depend on a warm cache. Cache misses may reduce performance, but they must not change build outcomes.

## 8. Release Identity Requirements

### 8.1 Identity categories

The system must treat the following identities as distinct:

- monorepo commit SHA
- user-facing release label
- official release identity
- CI build identity
- Docker tag set
- deployment target identity

These identities must be linked through traceability, but they must not be treated as interchangeable.

### 8.2 Official release definition

- Only intentionally promoted releases are official releases.
- Successful `main` builds are traceable build candidates, not official releases.
- Official releases must be intentionally promoted, permanently traceable, expose a user-friendly release label, and remain anchored to an immutable commit SHA.

### 8.3 Release identity model

- Manual version numbers must not be stored or maintained in source code as part of the normal monorepo release flow.
- Official release identity must be derived from immutable git information, with the monorepo commit SHA as the primary source of truth.
- Each official release must also expose a user-friendly release label such as `1.2.1` for human communication, release notes, and dashboards.
- The user-facing release label may be generated or assigned during the release process, but it must not become the internal source of truth in place of the commit SHA.
- Internal shared libraries such as `common` must follow the same repository-SHA model and must not require standalone version bump steps just to enable atomic monorepo changes.
- Official release records must remain component-scoped so a release can still be attributed unambiguously to Auth, Items, API Gateway, or Frontend.

### 8.4 Meaning of release ordering

- Whether release X is newer than release Y for users and operators must be determined by the official user-facing release label within the same component release stream.
- Internally, traceability and identity resolution must still rely on immutable commit SHA, build metadata, and image identity.
- Ordering comparisons are meaningful only within the same component release stream.
- Branch tags, SHA tags, and build identifiers are immutable identities, not user-maintained semantic versions.

## 9. Docker Tagging Requirements

### 9.1 Repository naming

- Each releasable deployable component must have its own image repository.
- Repository naming must follow the `<registry>/<service-name>` model.

### 9.2 Tag categories

Every produced image must use a tag model with clear purpose separation:

- immutable traceability tag
- optional mutable convenience tag
- official release tag when applicable

### 9.3 Required immutable tags

- Every image build must publish an immutable SHA-based tag.
- The SHA-based tag is the minimum guaranteed lookup key for commit-to-image tracing.

### 9.4 Feature-branch tags

- Feature-branch images must be pushed to the registry.
- Feature-branch images must include an immutable SHA tag and may include a branch-scoped convenience tag.
- Feature-branch tags are non-official artifacts and may expire.

### 9.5 Main-branch candidate tags

- Successful `main` builds must publish immutable SHA-based tags.
- `main` builds may publish a mutable convenience pointer for the latest main candidate.
- Mutable main-candidate pointers must never be used as the authoritative identity for deployment or rollback.

### 9.6 Official release tags

- Official releases must publish both an immutable SHA-based tag and a user-friendly release tag.
- The user-friendly release tag must be appropriate for human use, for example `1.2.1` or a component-scoped equivalent when needed for clarity.
- The immutable SHA-based tag or digest remains the internal source of truth for traceability, deployment identity resolution, and reverse lookup.
- If a global `latest` tag exists, it must point to the latest promoted official release, not merely to the newest successful `main` build.

### 9.7 Deployment identity rule

- Staging and production deployments must resolve to immutable tags or image digests.
- Mutable tags must never be the sole deployment identity.

## 10. Traceability Requirements

### 10.1 Required traceability chain

The system must support lossless lookup across the following chain:

- commit
- CI run
- built artifact
- image digest
- release tag
- deployed environment

### 10.2 Required metadata

Every build must retain enough metadata to answer all of the following:

- which monorepo commit SHA produced this image
- which user-facing release label was assigned, if any
- which CI run produced this image
- which branch or merge candidate was validated
- which component release identity was assigned
- which internal shared-library revisions from the same repository snapshot were used
- whether this artifact was only a branch build, a `main` candidate, or an official release

### 10.3 Image metadata requirement

- Images must carry standard OCI metadata and project-specific traceability metadata sufficient for reverse lookup.

### 10.4 Release manifest requirement

- Every official release must automatically generate and retain both a machine-readable release manifest and an SBOM or provenance artifact using established tooling where possible.
- Manual BOM or release-manifest maintenance is not acceptable for official releases.
- The release manifest must record the exact released components, source commit SHA, immutable image identifiers, and official release identity.
- The SBOM or provenance artifact must be attached to the official release record.
- The retained release metadata must allow reverse lookup from a release identity to the exact component set and exact immutable artifacts.

### 10.5 Reverse lookup requirement

- From a commit SHA, it must be possible to find the relevant build and produced image.
- From a release identity, it must be possible to find the exact released components, build evidence, release manifest, SBOM or provenance artifact, and immutable image identities.
- From a running deployment, it must be possible to identify the precise released or candidate artifact in use.

## 11. Retention Requirements

### 11.1 Non-official artifacts

- ECR lifecycle policies must automatically clean up feature-branch and `main` candidate images according to the defined retention rules.
- Feature-branch build logs, reports, and images may expire after approximately 30 days.
- `main` candidate build evidence may expire after approximately 30 days unless retained for a specific release or audit purpose.

### 11.2 Official release artifacts

- Official release metadata and audit evidence must be retained indefinitely.
- Indefinite retention for official releases applies to release records, immutable release identities, release manifests, SBOM or provenance artifacts, and associated build and test evidence required for audit and debugging.
- ECR lifecycle policies must keep the most recent 10 official release images in ECR for v1.
- Keeping only the most recent 10 official release images in ECR defines the immediate rollback window for v1. Older releases may remain fully traceable even when their images are no longer kept in ECR.

## 12. Security And Secrets Requirements For CI

- GitHub must authenticate to AWS using OIDC rather than long-lived AWS access keys.
- Secrets for the build and release flow must live in AWS Secrets Manager.
- The solution must rely on built-in secret masking and log-protection mechanisms rather than custom masking logic where possible.

## 13. Notification And Audit Requirements For Build Phase

- Notifications must be available to the sole project owner for build success, build failure, release readiness, and release failure events.
- Slack is the preferred operational notification channel.
- Email must remain available for important approval-oriented flows.
- Build dashboards must be visible from phone and laptop through low-cost or built-in tooling.
- The build system must preserve an audit trail of who approved, released, or promoted artifacts and when that happened.

## 14. Cost Constraints For Build Phase

- The preferred steady-state operating target is $1-$2 per month.
- $5 per month is the upper bound for v1.
- Where tradeoffs are required, the system must prioritize low cost first, then fast feedback, then AWS learning value.
- Features that threaten the cost ceiling may be simplified before traceability and release-safety requirements are weakened.
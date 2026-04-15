# Deployment Requirements

## 1. Purpose

This document defines the requirements for deploying OnlineShop artifacts to staging and production after the build and release requirements in [01_REQUIREMENTS_BUILD.md](./01_REQUIREMENTS_BUILD.md) are satisfied.

## 2. Scope

The deployment phase covers:

- staging and production environment behavior
- deployment approvals
- rollback behavior
- deployment notifications
- environment lifecycle cost controls
- deployment auditability

## 3. Environment Model

### 3.1 Required environments

- v1 requires staging and production environments.
- Staging exists to validate release candidates before production approval.

### 3.2 Environment cost behavior

- Staging does not need to be always on.
- On-demand staging is acceptable and preferred if it materially reduces cost.
- The deployment design must prefer on-demand or short-lived non-production environments over always-on cost.

### 3.3 Access model

- Private-only access is not a hard requirement for v1.
- If public exposure materially reduces cost and operational complexity, public access is acceptable for early environments.

## 4. Deployment Target Requirements

- The preferred v1 deployment platform is ECS.
- The deployment solution must favor the simplest ECS model that satisfies the cost cap and minimizes platform-maintenance overhead.
- If always-on ECS staging or production would violate the v1 cost ceiling, non-production environments must be simplified before abandoning traceability or release-safety requirements.

## 5. Deployment Promotion Rules

### 5.1 Promotion sequence

- Artifacts must move through a controlled progression from validated build candidate to staging to production.
- Production deployment must occur only from a successfully validated release candidate.

### 5.2 Pre-production validation

- Blocking e2e validation must already have passed before merge.
- After deployment to staging, end-to-end verification must run again against the staged environment before production approval.

### 5.3 Production approval

- Production deployment requires manual approval.
- Only the project owner may approve production deployment in v1.

## 6. Rollback Requirements

- Rollback must target a previously known-good immutable artifact.
- Rollback must not depend on mutable image tags.
- The v1 immediate rollback guarantee applies to official release images retained under the build-phase ECR lifecycle policy.
- Rollback requires manual approval in v1.
- Only the project owner may approve rollback in v1.
- The deployment system must make it easy to identify the previous known-good release and redeploy it safely.

## 7. Deployment Order And Compatibility

- v1 may allow services to deploy in parallel.
- v1 does not require backward- and forward-compatibility guarantees across independently deployed services.
- The deployment design must keep room for stricter compatibility rules later without invalidating release traceability.

## 8. Data And State Requirements

- v1 has minimal persistence and recovery requirements.
- Managed backup, disaster recovery, and strong durability guarantees are out of scope for v1.
- The deployment solution may use the simplest acceptable low-cost persistent-state approach for early environments.
- The requirements must explicitly assume that backup sophistication is deferred rather than silently omitted.

## 9. Notification Requirements For Deployment

- The owner must receive notifications for staging deployment results, production approval requests, production deployment results, rollback approval requests, and rollback results.
- Slack is the preferred primary operational channel.
- Email must remain available for approval-oriented flows and critical fallbacks.

## 10. Audit Requirements For Deployment

- The deployment system must record who approved production deployment, who approved rollback, what artifact was deployed, and when each action happened.
- Running environments must expose enough deployment metadata to identify the exact immutable artifact currently deployed.

## 11. Cost Constraints For Deployment

- The v1 monthly upper bound remains $5.
- If the full desired deployment shape cannot fit under that cap, the first sacrifices are always-on staging, private-only access, and extended non-release retention.
- ECS, notifications, dashboards, and release traceability remain preferred requirements and should not be dropped before the simpler cost-saving levers are exhausted.

## 12. Region Selection Requirement

- The initial AWS region does not need to be locked in this document.
- Region selection during implementation must prioritize lower cost among suitable EU regions.
- If cost is effectively equivalent, a region geographically closer to Bulgaria is preferred.
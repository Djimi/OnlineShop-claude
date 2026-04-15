# Automatic Builds And Deployment Requirements

This directory contains the polished requirements for automated build, test, release, deployment, and hosting flows for the OnlineShop project.

The requirements are intentionally split by phase so build automation can be reviewed independently from deployment and hosting:

1. [01_REQUIREMENTS_BUILD.md](./01_REQUIREMENTS_BUILD.md) defines CI, test gating, versioning, tagging, traceability, retention, and build-time cost constraints.
2. [02_REQUIREMENTS_DEPLOY.md](./02_REQUIREMENTS_DEPLOY.md) defines staging and production deployment requirements, approvals, rollback, notifications, and environment lifecycle constraints.
3. [03_REQUIREMENTS_HOSTING.md](./03_REQUIREMENTS_HOSTING.md) defines frontend hosting, public access expectations, domains, HTTPS, dashboards, and operational visibility requirements.

## Shared Decision Summary

- Cost is the primary constraint. The preferred operating target is $1-$2 per month, and $5 per month is the upper bound for v1.
- GitHub Actions is the primary CI system for v1.
- GitHub Actions dependency caching and Docker layer caching are mandatory for v1.
- Direct pushes to `main` are forbidden. Changes reach `main` only through protected pull requests.
- Blocking e2e validation must happen before merge, but not on every feature-branch push.
- Only intentional releases are official releases. Successful `main` builds are traceable release candidates, not official releases.
- Official releases must have user-friendly release labels for humans and SHA-based identities for internal traceability, without requiring manually maintained source-code versions.
- Versioning, tagging, and commit-to-image traceability are first-class requirements and are specified in detail in [01_REQUIREMENTS_BUILD.md](./01_REQUIREMENTS_BUILD.md).
- Deployment and hosting requirements must defer or simplify anything that threatens the cost ceiling before they compromise traceability or release safety.
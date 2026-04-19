# Hosting And Visibility Requirements

## 1. Purpose

This document defines hosting, frontend delivery, visibility, domain, HTTPS, and operational dashboard requirements for the OnlineShop project.

## 2. Frontend Hosting Requirements

- The frontend must be hosted separately from backend containers.
- The preferred frontend hosting model for v1 is S3 plus CloudFront.
- The hosting model must remain compatible with low-cost static-site delivery.

## 3. Domain Requirements

- A custom domain is not required for v1.
- The initial hosted version may use the simplest low-cost addressing model available.
- If a custom domain is introduced later, the hosting design must not require abandoning the versioning and traceability model established in [01_REQUIREMENTS_BUILD.md](./01_REQUIREMENTS_BUILD.md).

## 4. HTTPS Requirements

- HTTPS is not a hard requirement for v1.
- If HTTPS can be added with negligible cost and low operational overhead, it is preferred.

## 5. Access Model Requirements

- Public access is acceptable for v1 if it materially lowers cost and operational complexity.
- Private access control is optional for v1 and may be deferred if it threatens the cost ceiling.

## 6. Dashboard And Visibility Requirements

### 6.1 Build and deployment visibility

- The project owner must be able to inspect build status, logs, deployed versions, approval actions, and cost visibility from phone and laptop.
- Built-in low-cost dashboards are preferred for v1.

### 6.2 Tooling expectations

- Native GitHub and AWS dashboards are sufficient baseline requirements for v1.
- Additional custom dashboards are optional and should only be introduced if they remain low-cost and low-maintenance.

### 6.3 Audit visibility

- It must be possible to see who built, promoted, deployed, or rolled back a version and when that happened.

## 7. Runbook Requirements

- Runbooks and failure-investigation guidance must live in version-controlled repository documentation.
- Runbooks must cover, at minimum, build failure investigation, deployment approval flow, rollback approval flow, and version traceability lookup.

## 8. Cost Constraints For Hosting And Visibility

- Hosting and dashboard choices must honor the same $5 per month upper bound defined for v1.
- If necessary to stay within budget, public access, built-in dashboards, and minimal domain requirements take precedence over private access or custom dashboard development.
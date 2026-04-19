# Automatic Builds & Deploy — PLAN

## Strategy: Iterative, Fast Feedback

This plan is split into **4 passes**, each building on the previous one. The guiding principle is **deploy to AWS as fast as possible**, then harden and polish incrementally.

| Pass | Name | What You Get |
|------|------|--------------|
| 1 | **MVP: Running on AWS** | All services live on AWS. Manual deploys. Proves the system works in the cloud. |
| 2 | **CI Pipeline Hardening & Staging** | Automated CI on push/PR. Test gates. Selective builds. Branch protection. Staging environment. |
| 3 | **Release, Traceability & Promotion** | Official release model. Staging → production promotion with approval. Rollback. Full traceability. |
| 4 | **Operational Maturity** | Notifications. Dashboards. Runbooks. Nightly validation. Merge queue. Cost guardrails. |

After Pass 1 you have a **working deployment**. After Pass 4 you satisfy **every v1 requirement**.

---

## Subplans

1. [01_MVP_DEPLOY.md](./01_MVP_DEPLOY.md) — AWS account, ECR, minimal GH Actions, ECS Fargate, S3+CloudFront, databases
2. [02_CI_PIPELINE_HARDENING.md](./02_CI_PIPELINE_HARDENING.md) — Branch protection, selective builds, test gates, Docker tagging, caching, staging
3. [03_RELEASE_TRACEABILITY.md](./03_RELEASE_TRACEABILITY.md) — Release identity, promotion flow, production env, rollback, traceability chain, ECR retention
4. [04_OPERATIONAL_MATURITY.md](./04_OPERATIONAL_MATURITY.md) — Notifications, dashboards, audit, merge queue, nightly builds, runbooks, cost monitoring

---

## Requirements Source

All requirements come from these three documents in this directory:

- [01_REQUIREMENTS_BUILD.md](./01_REQUIREMENTS_BUILD.md) — CI, test gating, versioning, tagging, traceability, retention
- [02_REQUIREMENTS_DEPLOY.md](./02_REQUIREMENTS_DEPLOY.md) — Staging/production, approvals, rollback, notifications
- [03_REQUIREMENTS_HOSTING.md](./03_REQUIREMENTS_HOSTING.md) — Frontend hosting, domains, HTTPS, dashboards

---

## Cost Trajectory

| After Pass | Estimated Monthly Cost |
|---|---|
| 1 — MVP | ~$2–4 |
| 2 — + Staging | ~$3–5 |
| 3 — + Production + Release infra | ~$4–5 (staging on-demand keeps it in budget) |
| 4 — + Monitoring/notifications | ~$4–5 (no incremental AWS cost) |

The $5/month ceiling is respected throughout by:
- Using Fargate Spot pricing
- Making staging on-demand (scale to 0 or tear down when idle)
- Leveraging RDS Free Tier (12 months)
- Using GitHub Actions free tier (2000 min/month)
- Relying on built-in dashboards (no custom tooling)

---

## Key Decisions to Make During Implementation

| Decision | When | Options |
|---|---|---|
| AWS region | Pass 1 | `eu-north-1` (Stockholm) vs `eu-central-1` (Frankfurt) — pick cheapest |
| Database: RDS vs containerized PG | Pass 1 | RDS Free Tier preferred; containerized PG if post-Free-Tier cost is a concern |
| Routing: ALB vs Service Connect | Pass 1 | ALB is simpler; Service Connect is cheaper — evaluate during implementation |
| Staging lifecycle model | Pass 2 | Scale-to-zero vs on-demand teardown |
| Release label strategy | Pass 3 | Auto-generated (semantic-release) vs manually assigned during promotion |

---

## Progress

- [ ] **Pass 1** — MVP: Running on AWS
- [ ] **Pass 2** — CI Pipeline Hardening & Staging
- [ ] **Pass 3** — Release, Traceability & Promotion
- [ ] **Pass 4** — Operational Maturity

# Pass 1 — MVP: Running on AWS

**Goal:** Get the OnlineShop accessible on AWS with the shortest path possible. No polish, no automation beyond the bare minimum. Prove the system works in the cloud.

**Exit criteria:** All four components (Auth, Items, API Gateway, Frontend) are reachable on AWS. A developer can manually trigger a deployment.

---

## Tasks

### 1.1 AWS Account & OIDC Foundation

- [ ] Create (or confirm) AWS account under the Free Tier
- [ ] Select initial AWS region (cheapest suitable EU region — likely `eu-north-1` Stockholm or `eu-central-1` Frankfurt)
- [ ] Set up GitHub → AWS OIDC trust (no long-lived access keys — req 01 §12)
- [ ] Create an IAM role scoped to ECR push, ECS deploy, S3/CloudFront management
- [ ] Store any required secrets in AWS Secrets Manager

### 1.2 Container Registry (ECR)

- [ ] Create ECR repositories: `auth`, `items`, `api-gateway`
- [ ] Confirm each service Dockerfile builds locally and produces a working image
- [ ] Push images manually (or via a trivial GH Actions workflow) with a SHA-based tag

### 1.3 Minimal GitHub Actions — Build & Push

- [ ] Create a single reusable workflow (or one per service) that:
  - Builds the Maven project
  - Builds the Docker image
  - Tags with `sha-<SHORT_SHA>`
  - Pushes to ECR
- [ ] Enable GitHub Actions dependency caching for Maven (`.m2/repository`)
- [ ] Enable Docker layer caching (e.g., `docker/build-push-action` with GHA cache backend)
- [ ] Trigger: manual (`workflow_dispatch`) is sufficient for this pass

### 1.4 Database Layer

- [ ] Decide: RDS Free Tier PostgreSQL vs. containerized PostgreSQL on ECS
  - RDS Free Tier (`db.t3.micro` / `db.t4g.micro`, 20 GB) is free for 12 months — **preferred for MVP**
  - Containerized PG is cheaper long-term but no managed backups
- [ ] Provision PostgreSQL instance(s) for Auth and Items
- [ ] Apply init-db scripts (`01-schema.sql`, `02-seed-data.sql`) to each database
- [ ] Store DB credentials in Secrets Manager; wire ECS task definitions to fetch them

### 1.5 ECS Cluster & Services (Fargate)

- [ ] Create ECS cluster
- [ ] Create task definitions for Auth, Items, API Gateway
  - Fargate Spot where possible (cost savings)
  - Minimal resource allocation (0.25 vCPU / 0.5 GB per task is a good starting point)
- [ ] Create ECS services (desired count = 1 per service)
- [ ] Set up a single ALB (Application Load Balancer) with path-based routing:
  - `/api/auth/**` → Auth service
  - `/api/items/**` → Items service
  - `/api/**` → API Gateway (or route everything through Gateway)
- [ ] Configure security groups: ALB ↔ ECS tasks ↔ RDS
- [ ] Verify backend health checks pass

### 1.6 Frontend — S3 + CloudFront

- [ ] Create S3 bucket for frontend static assets
- [ ] Build frontend (`npm run build`) and upload `dist/` to S3
- [ ] Create CloudFront distribution pointing to S3 origin
- [ ] Configure CloudFront to forward `/api/*` requests to the ALB (origin for backend)
- [ ] Verify frontend loads and can communicate with backend APIs

### 1.7 Smoke Test

- [ ] Manually verify: frontend loads, login works, items list renders
- [ ] Document the public URL(s) in the project README or a dedicated `DEPLOYED.md`

---

## Cost Estimate (MVP)

| Resource | Estimated Monthly Cost |
|---|---|
| ECS Fargate (3 tasks, minimal) | ~$1–3 (Spot pricing) |
| ALB | ~$0.50 (minimal traffic) |
| RDS Free Tier | $0 (12 months) |
| ECR | ~$0.10 |
| S3 + CloudFront | ~$0.10 (minimal traffic) |
| **Total** | **~$2–4** |

> If the ALB pushes cost above $5, consider replacing it with direct ECS Service Connect or a cheaper routing approach (evaluated in Pass 2).

---

## Out of Scope for Pass 1

- Automated CI triggers on push/PR (Pass 2)
- Selective builds / change detection (Pass 2)
- Test gates in CI (Pass 2)
- Branch protection enforcement (Pass 2)
- Staging environment (Pass 2)
- Release identity model (Pass 3)
- Notifications, dashboards, runbooks (Pass 4)

# WHAT WAS DONE — MVP Deploy Pass 1

> Track every action so future automation can reproduce it.

---

## Step 1.1 — AWS Account & OIDC Foundation ✅

- AWS account `799111666795`, region `eu-north-1` (Stockholm)
- IAM user `admin` for manual CLI operations (profile: `dpm-profile`)
- GitHub → AWS OIDC trust configured via IAM role

## Step 1.2 — Container Registry (ECR) ✅

Three ECR repositories created:
| Repository | URI |
|---|---|
| `onlineshop-auth` | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-auth` |
| `onlineshop-items` | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-items` |
| `onlineshop-api-gateway` | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-api-gateway` |

Naming convention: `onlineshop-<service>` (no slashes, fixed from initial mistake).

## Step 1.3 — GitHub Actions Build & Push ✅

Workflow: `.github/workflows/build-and-push.yml`
- Trigger: `workflow_dispatch` with service input (`auth`/`items`/`api-gateway`/`all`)
- OIDC auth → ECR login → Maven build → Docker build → push with `sha-<FULL_SHA>` tag
- Items job builds `common` first (dependency)
- Maven caching via `actions/cache@v4` with `pom.xml` hash keys
- Docker layer caching via BuildKit (`setup-buildx-action` + `type=gha`)

IAM role for GitHub Actions: `arn:aws:iam::799111666795:role/github-actions-onlineshop`
- Trust policy: OIDC from `repo:Djimi/OnlineShop-claude:*`
- Inline policy: `ecr-push-pull` for ECR operations

## Step 1.4a — RDS Provisioning ✅

| Property | Value |
|---|---|
| Instance ID | `onlineshop-postgres-db` |
| Endpoint | `onlineshop-postgres-db.cf2gikqaqh9f.eu-north-1.rds.amazonaws.com` |
| Port | 5432 |
| Engine | PostgreSQL 18.4 |
| Instance class | db.t4g.micro |
| Storage | 20 GB, encrypted |
| Initial DB | `auth` |
| Public access | **No** (false) |
| Security group | `sg-04ba95188d8374d96` |
| Subnet group | `default-vpc-06eeb0bc47ecdbd61` |
| Multi-AZ | No |

No credentials in AWS Secrets Manager yet.

---

## Step 1.4b — Apply init-db scripts ✅

- [x] Create `items` database on RDS
- [x] Apply Auth DDL: `Auth/init-db/01-schema.sql` → table `users` + `sessions` on database `auth`
- [x] Apply Auth seed: `Auth/init-db/02-seed-data.sql` → 1 test user (`testuser`)
- [x] Apply Items DDL: `Items/init-db/01-schema.sql` → table `items` on database `items`
- [x] Apply Items seed: `Items/init-db/02-data.sql` → 5 seed products
- [x] Create least-privilege service accounts (not root `dbadmin`):
  - `auth_app` — access only to `auth` database (SELECT/INSERT/UPDATE/DELETE on all tables)
  - `items_app` — access only to `items` database (SELECT/INSERT/UPDATE/DELETE on all tables)
- [x] Store service credentials in AWS Secrets Manager:
  - `onlineshop/auth/db` — ARN: `arn:aws:secretsmanager:eu-north-1:799111666795:secret:onlineshop/auth/db-umtxh1`
  - `onlineshop/items/db` — ARN: `arn:aws:secretsmanager:eu-north-1:799111666795:secret:onlineshop/items/db-bM5eSY`

**Connectivity method:** `docker run --rm -e PGPASSWORD=$PASS postgres:18-alpine psql -h $HOST -U $USER -d $DB`
No `psql` client installed locally; used Postgres Docker image as client.
Master user: `dbadmin`, password stored in local `.env` (`POSTGRES_AWS_SECRET`). Root user NOT used by apps.

---

## Step 1.5 — Pre-Requisite Code Changes ✅

### Items: Spring Boot Actuator added
- Added `spring-boot-starter-actuator` dependency to `Items/pom.xml`
- Added management endpoints config to `Items/src/main/resources/application.yml`:
  - `/actuator/health` with `show-details: always`
  - Liveness/readiness probe groups
  - Custom health group: `[db]`
  - Metrics tagged with `application: items`
- **Security fixes** from code review:
  - Restricted exposure from `"*"` → `health,metrics` (prevents DB password leak via `/actuator/env`)
  - `env.show-values` / `configprops.show-values` → `never`
  - Removed dead singular `endpoint` config block
  - Removed `prometheus` from exposure (no dependency)
- Fixed pre-existing test bugs (import paths, missing mocks) — 72 tests pass

### API Gateway: Redis resilience for sidecar
- `RateLimitFilter.java`: fail-open on Redis errors (was returning 500s)
- `RateLimitConfig.java`: `@Lazy` proxy manager + `RedisURI` with bounded timeouts (was unlimited)
- `application.yml`: added `spring.data.redis.connect-timeout: 10s`
- 10 tests pass

---

## Step 1.5 — ECS Infrastructure ✅ (provisioned, not fully working)

### ECS Cluster
- `onlineshop-cluster` (Fargate), ACTIVE

### IAM
- `ecsTaskExecutionRole` — ECR pull + CloudWatch logs + Secrets Manager read

### Security Groups
| SG | ID | Rules |
|----|-------|-------|
| ALB | sg-0b5427a6a3bf31c29 | inbound :80 from 0.0.0.0/0 |
| ECS | sg-0b209104a6b15b157 | inbound :0-65535 from ALB SG |
| DB | sg-04ba95188d8374d96 | inbound :5432 from ECS SG |

**MISSING:** ECS SG self-referencing rule for ports 9000-9001 (blocks API Gateway → Auth/Items)

### Cloud Map
- Namespace `onlineshop.local` (private DNS, VPC vpc-06eeb0bc47ecdbd61)
- Services: `auth-port`, `items-port`, API Gateway client

### Task Definitions
| Service | Latest Rev | Image | Notes |
|---------|------------|-------|-------|
| Auth | 3 | sha-befc22... | HikariCP 10, Secrets Manager |
| Items | 4 | sha-ba7905d | Actuator, HikariCP 10 |
| API Gateway | 7 | sha-ba7905d | Redis sidecar, rate-limit off, Auth IP hardcoded |

### ALB
- `onlineshop-alb` → DNS: `onlineshop-alb-199112777.eu-north-1.elb.amazonaws.com`
- Target group: `onlineshop-gateway-tg` (port 10000, IP type)
- Listener: :80 → forward to gateway-tg

### ECS Services
All 3: 1 running, HEALTHY, Service Connect enabled

### Fixes Applied During Deployment

1. **Self-referencing SG rules** — Added inbound tcp:9000-9001 + tcp:6379 on `sg-0b209104a6b15b157` from itself → API Gateway now reaches Auth/Items/Redis
2. **Service Connect DNS** — `auth.onlineshop.local` still not resolving. Hardcoded private IPs in `SPRING_APPLICATION_JSON` as workaround
3. **Resilience4j TimeLimiter** — Auth validation timeout: 3s → 5s in `ResilienceConfig.java` (ECS task-to-task latency higher than localhost)
4. **Rate limiting disabled** — `GATEWAY_RATELIMIT_ENABLED=false` because `RateLimitConfig.bucket4jProxyManager` connects to Redis eagerly
5. **HikariCP pool** — Auth: 100 → 10 connections (RDS `db.t4g.micro` max ~25 connections)

### Verified Working
- Register new user: `POST /auth/register` → 201
- Login: `POST /auth/login` → 200 with token
- List items: `GET /items` with Bearer token → 200, 5 products
- Token validation: `GET /auth/validate` → 200
- ALB health check: `GET /actuator/health` → 200 UP
- API Gateway rev 11 (sha-ba7905d), Auth rev 3, Items rev 4

### Remaining Tech Debt
- Service Connect DNS (pass 2)
- Rate limiter lazy Redis connection (pass 2)
- Items task private IP changes on restart will break Gateway routing (need dynamic discovery)
- Frontend not deployed yet (step 1.6)


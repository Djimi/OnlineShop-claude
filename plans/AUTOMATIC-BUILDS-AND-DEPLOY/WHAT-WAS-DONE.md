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

## Step 1.4b — Apply init-db scripts (IN PROGRESS)

- [ ] Create `items` database on RDS
- [ ] Apply Auth DDL: `Auth/init-db/01-schema.sql` → database `auth`
- [ ] Apply Auth seed: `Auth/init-db/02-seed-data.sql` → database `auth`
- [ ] Apply Items DDL: `Items/init-db/01-schema.sql` → database `items`
- [ ] Apply Items seed: `Items/init-db/02-data.sql` → database `items`
- [ ] Store DB credentials in AWS Secrets Manager


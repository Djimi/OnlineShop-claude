# AWS Commands Guide — OnlineShop Deployment

> **Goal:** Document every AWS CLI command used during the deployment setup, so you understand **what** it does, **why** we run it, and **how** to interpret the output.  
> **AWS Profile:** Using admin credentials (user `arn:aws:iam::799111666795:user/admin`)  
> **Region:** `eu-north-1` (Stockholm)  
> **Account ID:** `799111666795`

---

## Table of Contents

- [1. Identity & Context](#1-identity--context)
- [2. OIDC Provider (GitHub → AWS Trust)](#2-oidc-provider-github--aws-trust)
- [3. IAM Roles & Policies](#3-iam-roles--policies)
- [4. ECR (Elastic Container Registry)](#4-ecr-elastic-container-registry)
- [5. Manually Pushing Images to ECR](#5-manually-pushing-images-to-ecr)
- [6. Common Troubleshooting](#6-common-troubleshooting)

---

## 1. Identity & Context

### `aws sts get-caller-identity`

```bash
aws sts get-caller-identity
```

| Field | Meaning |
|-------|---------|
| `UserId` | Unique ID of the IAM user/role you're calling as |
| `Account` | The 12-digit AWS account ID |
| `Arn` | Full ARN identifying who you are (user, role, etc.) |

**Why run this:** Always the first command to confirm you're authenticated and in the right account. No side effects — purely a read.

**Example output:**

```json
{
    "UserId": "AIDA3UDWELRV3TXMFS5B3",
    "Account": "799111666795",
    "Arn": "arn:aws:iam::799111666795:user/admin"
}
```

→ "I am logged in as `admin` in account `799111666795`."

---

## 2. OIDC Provider (GitHub → AWS Trust)

### Background: What is OIDC?

**OIDC = OpenID Connect.** In this context, it lets GitHub Actions authenticate to AWS *without storing long-lived AWS access keys* (which is a security risk).

**How it works:**

```
GitHub Actions workflow
    ↓ (1) Requests an OIDC token from GitHub
    ↓ (2) Gives that token to AWS
AWS IAM
    ↓ (3) Validates the token against the OIDC provider
    ↓ (4) Returns temporary AWS credentials
```

No secrets stored anywhere — GitHub is the identity provider, AWS trusts it.

### `aws iam list-open-id-connect-providers`

```bash
aws iam list-open-id-connect-providers
```

**Why run this:** To check whether the GitHub OIDC provider is already registered in this AWS account.

**Output:** List of OIDC provider ARNs, or empty list `[]` if none exist.

Add `--query` to filter:

```bash
aws iam list-open-id-connect-providers --query "OpenIDConnectProviderList[*].Arn"
```

### `aws iam get-open-id-connect-provider`

```bash
aws iam get-open-id-connect-provider \
  --open-id-connect-provider-arn "arn:aws:iam::799111666795:oidc-provider/token.actions.githubusercontent.com"
```

**Why run this:** To inspect the OIDC provider's configuration — which audience it expects, which thumbprint it uses.

| Field | Meaning |
|-------|---------|
| `Url` | The identity provider URL (`token.actions.githubusercontent.com`) |
| `ClientIDList` | The audience value. Must match `aud` claim in tokens. GitHub Actions uses `sts.amazonaws.com` |
| `ThumbprintList` | SHA-1 fingerprint of the provider's TLS certificate, used for identity verification |

**Key check:** `ClientIDList` must contain `sts.amazonaws.com`. If it contained `sigstore` instead, the OIDC flow would fail.

---

## 3. IAM Roles & Policies

### Background: IAM Role vs. IAM User

| | IAM User | IAM Role |
|---|---|---|
| Has permanent credentials? | Yes (access key + secret) | No (temporary only) |
| Who uses it? | Humans, long-lived services | AWS services, federated identities (GitHub) |
| Security | Credentials can leak | No credentials to leak — assumed via trust |

For GitHub Actions, we create an **IAM Role** with a **Trust Policy** that allows GitHub to assume it via OIDC.

### The Trust Policy

This is what we wrote (formatted for readability):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Federated": "arn:aws:iam::799111666795:oidc-provider/token.actions.githubusercontent.com"
      },
      "Action": "sts:AssumeRoleWithWebIdentity",
      "Condition": {
        "StringEquals": {
          "token.actions.githubusercontent.com:aud": "sts.amazonaws.com"
        },
        "StringLike": {
          "token.actions.githubusercontent.com:sub": "repo:Djimi/OnlineShop-claude:*"
        }
      }
    }
  ]
}
```

**Key parts explained:**

| Part | Meaning |
|------|---------|
| `Principal.Federated` | "Only the GitHub OIDC provider can assume this role" |
| `Action` → `sts:AssumeRoleWithWebIdentity` | The specific AWS API call GitHub will make |
| `Condition` → `aud` | The audience claim must be `sts.amazonaws.com` (matches what GitHub sends) |
| `Condition` → `sub` | The subject claim must match `repo:Djimi/OnlineShop-claude:*` — only **your** repo can assume this role. The `*` means any branch/tag/ref. |

→ **In plain English:** "GitHub Actions running in the `Djimi/OnlineShop-claude` repo may assume this role and get temporary AWS credentials."

### `aws iam create-role`

```bash
aws iam create-role \
  --role-name github-actions-onlineshop \
  --assume-role-policy-document "file://trust-policy.json" \
  --description "OIDC role for GitHub Actions in OnlineShop repo"
```

**Why run this:** Creates the IAM role with the OIDC trust relationship.

**Key fields in output:**

| Field | Meaning |
|-------|---------|
| `RoleName` | `github-actions-onlineshop` — the name you'll reference everywhere |
| `Arn` | `arn:aws:iam::799111666795:role/github-actions-onlineshop` — the full identity. This goes into the GitHub Actions workflow YAML |
| `AssumeRolePolicyDocument` | Echoes back the trust policy you just provided |

**Important:** Creating the role only sets *who can assume it* (the trust policy). It doesn't give it any *permissions* (what it can do). That comes next.

### The ECR Permissions Policy

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:GetRepositoryPolicy",
        "ecr:DescribeRepositories",
        "ecr:ListImages",
        "ecr:DescribeImages",
        "ecr:BatchGetImage",
        "ecr:InitiateLayerUpload",
        "ecr:UploadLayerPart",
        "ecr:CompleteLayerUpload",
        "ecr:PutImage"
      ],
      "Resource": "*"
    }
  ]
}
```

**Key permissions grouped by purpose:**

| Purpose | Actions |
|---------|---------|
| Authenticate to ECR | `ecr:GetAuthorizationToken` |
| Push images | `ecr:InitiateLayerUpload`, `ecr:UploadLayerPart`, `ecr:CompleteLayerUpload`, `ecr:PutImage` |
| Pull images | `ecr:BatchCheckLayerAvailability`, `ecr:GetDownloadUrlForLayer`, `ecr:BatchGetImage` |
| Inspect repo | `ecr:DescribeRepositories`, `ecr:ListImages`, `ecr:DescribeImages`, `ecr:GetRepositoryPolicy` |

**Why `Resource: "*"` for Pass 1:** It means "all ECR repositories in this account." For production, you'd scope it to specific repository ARNs. But for MVP, this is simpler and sufficient.

### `aws iam put-role-policy`

```bash
aws iam put-role-policy \
  --role-name github-actions-onlineshop \
  --policy-name ecr-push-pull \
  --policy-document "file://ecr-policy.json"
```

**Why run this:** Attaches the ECR permissions to the role. This is an **inline policy** (embedded in the role) rather than a standalone managed policy.

**Output:** None on success. AWS CLI is silent when things work — that's normal.

### `aws iam list-role-policies`

```bash
aws iam list-role-policies --role-name github-actions-onlineshop
```

**Why run this:** Verify that the policy was actually attached. Always confirm after creating resources.

**Expected output:**

```json
{
    "PolicyNames": ["ecr-push-pull"]
}
```

### `aws iam list-roles` (with query filter)

```bash
aws iam list-roles --query "Roles[?contains(RoleName,'onlineshop')].{Name:RoleName,Arn:Arn}"
```

**Why run this:** Search for roles related to the project. The `--query` filter uses [JMESPath](https://jmespath.org/) syntax to filter and reshape the output.

**Without `--query`:** Returns up to 1000 roles in one giant JSON blob — hard to read.

---

## 4. ECR (Elastic Container Registry)

### Background: What is ECR?

ECR is AWS's private Docker image registry. Think of it as "Docker Hub, but private to your AWS account." It stores your container images, and ECS (Elastic Container Service) pulls from it to run your services.

### `aws ecr describe-repositories`

```bash
# Check repos in a specific region
aws ecr describe-repositories --region eu-north-1

# Check multiple regions (useful when you don't know which region was used)
aws ecr describe-repositories --region eu-north-1
aws ecr describe-repositories --region eu-central-1
aws ecr describe-repositories --region us-east-1
```

**Why run this:** To see what ECR repositories exist and their details.

**Key fields in output:**

| Field | Meaning |
|-------|---------|
| `repositoryName` | The name you use in `docker push` / `docker pull` |
| `repositoryUri` | The full push URL: `<account>.dkr.ecr.<region>.amazonaws.com/<name>` |
| `imageTagMutability` | `MUTABLE` means tags can be overwritten; `IMMUTABLE` means each tag can only be pushed once |
| `createdAt` | When the repo was created |

### `aws ecr describe-images`

```bash
aws ecr describe-images --repository-name "onlineshop-api-gateway" --region eu-north-1
```

**Why run this:** List all images (and their tags) in a repository. Empty `imageDetails: []` means no images have been pushed yet.

### `aws ecr create-repository`

```bash
aws ecr create-repository --repository-name "onlineshop-api-gateway" --region eu-north-1
```

**Why run this:** Creates a new private Docker registry repository for one service.

**Convention:** Each deployable service gets its own ECR repo. This keeps images isolated and makes lifecycle policies per-service.

### `aws ecr delete-repository`

```bash
aws ecr delete-repository --repository-name "onlineshop-auth/api-gateway" --region eu-north-1 --force
```

**Why run this:** Deletes a repository. The `--force` flag is needed if the repo contains images (otherwise it fails as a safety measure).

**Warning:** This is **destructive** — images inside are permanently deleted. Only do this for repos you're sure you don't need.

---

## 5. Manually Pushing Images to ECR

For reference — the GitHub Actions workflow automates these steps, but here's how you'd do it manually:

### Step 1: Authenticate Docker to ECR

```bash
aws ecr get-login-password --region eu-north-1 | docker login --username AWS --password-stdin 799111666795.dkr.ecr.eu-north-1.amazonaws.com
```

**What this does:**
- `aws ecr get-login-password` generates a temporary (12-hour) authentication token
- The token is piped to `docker login` which stores it in `~/.docker/config.json`
- `--username AWS` is always `AWS` for ECR — the actual auth is in the password token

### Step 2: Build the Docker image

```bash
cd Auth
docker build -t 799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-auth:sha-abc123 .
```

### Step 3: Push the image

```bash
docker push 799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-auth:sha-abc123
```

### Step 4: Pull the image (from another machine / ECS)

```bash
docker pull 799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-auth:sha-abc123
```

---

## 6. Common Troubleshooting

### "The specified value for assumeRolePolicyDocument is invalid"

**Cause:** The JSON file has non-ASCII characters (e.g., BOM, curly quotes).  

**Fix:** Write the file as pure ASCII using `[System.Text.Encoding]::ASCII` in PowerShell or `jq` on Linux/Mac.

### "AccessDenied" when GitHub Actions tries to assume the role

**Checklist:**
1. Is the OIDC provider set up in the correct AWS account? (`list-open-id-connect-providers`)
2. Does the trust policy's `sub` condition match your repo? (`repo:Djimi/OnlineShop-claude:*`)
3. Does the workflow have `permissions: id-token: write`? (Without this, GitHub won't give the workflow an OIDC token)
4. Is the role ARN correct in the workflow YAML?

### "repository does not exist" when pushing to ECR

**Checklist:**
1. Are you pushing to the right region? The region is encoded in the ECR URI: `dkr.ecr.<region>.amazonaws.com`
2. Is the repository name exactly correct (case-sensitive)?
3. Did you authenticate with `aws ecr get-login-password` first?

### Docker `chmod +x mvnw` fails on Windows

**This is normal.** The workflow runs on `ubuntu-latest`, not Windows. The `chmod` is needed on Linux to make the Maven wrapper executable. On your local Windows machine, you use `mvnw.cmd` instead — which works without `chmod`.

---

## Quick Reference: Key ARNs

| Resource | ARN |
|----------|-----|
| IAM Role | `arn:aws:iam::799111666795:role/github-actions-onlineshop` |
| OIDC Provider | `arn:aws:iam::799111666795:oidc-provider/token.actions.githubusercontent.com` |
| ECR — Auth | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-auth` |
| ECR — Items | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-items` |
| ECR — API Gateway | `799111666795.dkr.ecr.eu-north-1.amazonaws.com/onlineshop-api-gateway` |

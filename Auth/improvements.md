# Auth Stress Improvements Plan And Execution

## Goal
Increase concurrent request handling for Auth under `tests/performance/stress.js` while keeping:
- secure Argon2 settings
- existing API contracts
- DB-only architecture (no extra cache layer)

## Plan
- [x] Step 1: Remove infrastructure bottlenecks in perf environment (Auth/JVM/Postgres resources).
- [x] Step 2: Optimize service DB access path without changing contracts/security (projection + shorter transaction scope).
- [x] Step 3: Try deeper DB-path micro-optimization (native session insert), validate, and keep only if improved.
- [x] Step 4: Keep best-performing secure configuration and re-run to confirm.
- [x] Step 5: Increase compute/memory headroom further (as requested) and re-validate 300 VU behavior.

## Issues List
- [x] Connection starvation and transaction acquisition failures under stress.
  - Fixed by raising Auth/Postgres resource headroom and DB memory settings in perf compose.
- [x] Secure setup previously aborted early under low achieved throughput.
  - Fixed by infrastructure + DB-path tuning; full 20-minute stress run completes with 0% request failures.
- [x] One DB micro-optimization attempt regressed high-load latency.
  - Fixed by rolling back that step and preserving the better variant.

## Iteration Results

### Baseline (Secure + DB-only, failing)
- Source: `tests/performance/reports/stress-2026-02-09T09-18-37-579Z.json`
- What was the action exactly and why it was taken:
  - Baseline run before these iterations, used as reference for secure behavior.
- Results from that action:
  - `http_reqs`: 344
  - `checks`: 98.29%
  - p95: login 17726ms, validate 10408ms, register 15436ms
  - Fail rates: login 4.62%, validate 2.06%, register 0%
  - Test aborted early; system could not sustain ramp.

### Step 1 (Infrastructure headroom)
- What was the action exactly and why it was taken:
  - Updated `tests/performance/docker-compose.perf.yml`:
    - Auth JVM: `-Xmx512m -> -Xmx4g`, `-Xms1g`
    - Auth container limits: memory `1G -> 8G`, CPU `2 -> 6`
    - Postgres memory tuning increased (`shared_buffers`, `effective_cache_size`, `maintenance_work_mem`, `work_mem`)
  - Reason: secure Argon2 load and DB concurrency were hitting environment limits first.
- Results from that action:
  - Source: `tests/performance/reports/stress-2026-02-09T16-28-12-278Z.json`
  - `http_reqs`: 138,703
  - `checks`: 100%
  - p95: login 919.42ms, validate 577.66ms, register 918.03ms
  - Fail rates: 0% across login/validate/register
  - Improvement vs baseline: throughput massively increased and early failure disappeared.

### Step 2 (DB path optimization without contract/security change)
- What was the action exactly and why it was taken:
  - In `Auth/src/main/java/com/onlineshop/auth/service/AuthService.java`:
    - removed service-level `@Transactional` on `login`, `register`, `validateToken`
    - `validateToken` switched to lightweight projection-based repository query
  - In `Auth/src/main/java/com/onlineshop/auth/repository/SessionRepository.java`:
    - added projection query for validate path (`findValidationProjectionByTokenHash`)
  - Reason: reduce JDBC connection hold/lifecycle overhead on hot paths while keeping behavior identical.
- Results from that action:
  - Source: `tests/performance/reports/stress-2026-02-09T16-51-36-446Z.json`
  - `http_reqs`: 139,185
  - `checks`: 100%
  - p95: login 1040ms, validate 494.46ms, register 1040ms
  - Fail rates: 0% across login/validate/register
  - Improvement: validate crossed below 500ms aggregate threshold, stability preserved.

### Step 3 (Attempted deeper DB micro-optimization - reverted)
- What was the action exactly and why it was taken:
  - Tried native insert + projection-only login/validate flow:
    - `UserRepository` auth projection
    - `SessionRepository.insertSession(...)` native SQL
    - `AuthService.login()` switched to native insert path
  - Reason: attempt to remove remaining ORM overhead on the write path.
- Results from that action:
  - Source: `tests/performance/reports/stress-2026-02-09T17-14-00-686Z.json`
  - `http_reqs`: 138,331
  - `checks`: 99.99%
  - p95: login 882.19ms, validate 560.98ms, register 962.72ms
  - Minor failures appeared and validate regressed above 500ms aggregate.
  - Outcome: rejected and rolled back.

### Step 4 (Rollback to best variant + confirm)
- What was the action exactly and why it was taken:
  - Reverted Step 3 code and kept Step 2 architecture (projection validate + no service-level coarse transactions).
  - Rebuilt and reran stress to confirm reproducibility.
- Results from that action:
  - Source: `tests/performance/reports/stress-2026-02-09T17-36-07-998Z.json`
  - `http_reqs`: 139,599
  - `checks`: 100%
  - p95: login 968.59ms, validate 466.79ms, register 981.26ms
  - Fail rates: 0% across login/validate/register

### Step 5 (High-resource run per request)
- What was the action exactly and why it was taken:
  - Increased Auth resources in `tests/performance/docker-compose.perf.yml`:
    - JVM `-Xmx8g` / `-Xms2g`
    - container limits `16G` RAM, `12` CPUs (reservation `4G` / `4` CPUs)
  - Reason: user explicitly asked to keep security and iterate with larger resource budgets.
- Results from that action:
  - Source: `tests/performance/reports/stress-2026-02-09T17-59-46-341Z.json`
  - `http_reqs`: 152,815
  - `checks`: 100%
  - p95: login 503.90ms, validate 159.58ms, register 506.84ms
  - Fail rates: 0% across login/validate/register
  - Improvement vs Step 4:
    - throughput: `139,599 -> 152,815` (about `+9.47%`)
    - login p95: `968.59ms -> 503.90ms` (about `-47.98%`)
    - validate p95: `466.79ms -> 159.58ms` (about `-65.81%`)
    - register p95: `981.26ms -> 506.84ms` (about `-48.35%`)

## Final Result Summary
- Kept security intact (Argon2 unchanged).
- Kept contracts unchanged.
- Kept DB-only architecture (no new cache layer).
- Increased handled requests from 344 (failing baseline run) to 152,815 with 0% failures in full stress profile.
- All stress thresholds pass under secure settings.
- 300 VU latencies are still above 10 VU latencies, but the gap is substantially reduced and system remains stable through full 300 VU stages.

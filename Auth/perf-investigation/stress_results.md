# Auth Stress Results (k6 `stress.js`)

## Scope
- Test suite: `tests/performance/stress.js`
- Environment: `tests/performance/docker-compose.perf.yml` (Auth + Postgres + k6)
- Date: 2026-02-09
- Security stance: Argon2 remains secure (`Argon2PasswordEncoder(16, 32, 1, 47104, 1)`), no response-contract changes, no cache layer added in Auth.

## Current Thresholds
From `tests/performance/config/thresholds.js` (`generateStressThresholds`):
- `http_req_duration{operation:login}`: `p(95) < 2000ms`
- `http_req_duration{operation:validate}`: `p(95) < 500ms`
- `http_req_duration{operation:register}`: `p(95) < 2000ms`
- `http_req_failed{operation:login}`: `< 30%`
- `http_req_failed{operation:validate}`: `< 20%`
- `http_req_failed{operation:register}`: `< 40%`

During this investigation I executed with `EARLY_ABORT=true`, so active abort thresholds in `stress.js` were:
- validate p95 `< 2500ms` (abort on fail)
- login p95 `< 4000ms` (abort on fail)
- register p95 `< 4000ms` (abort on fail)
- validate error rate `< 30%` (abort on fail)

## How Results Were Collected And Analyzed
- Summary metrics were taken from generated files in `tests/performance/reports/stress-*.json`.
- Monitoring-point behavior was extracted from `tests/performance/reports/results.json` by slicing these windows:
  - 10 VU approximation: 0.4-0.8 minutes
  - 50 VU steady: 2-5 minutes
  - 100 VU steady: 7-10 minutes
  - 200 VU steady: 12-15 minutes
  - 300 VU steady: 16-18 minutes
- For each window and operation (`login`, `validate`, `register`), analysis used:
  - request count
  - average latency
  - p95 latency
  - failure rate (from `http_req_failed`)

## Monitoring-Point Behavior (Final Best Secure Run)
Source run: `tests/performance/reports/stress-2026-02-09T17-59-46-341Z.json`

### 10 VU approx (0.4-0.8m)
- `login`: reqs=45, avg=104.22ms, p95=138.56ms, fail=0%
- `validate`: reqs=263, avg=7.39ms, p95=11.91ms, fail=0%
- `register`: reqs=37, avg=108.21ms, p95=154.31ms, fail=0%

### 50 VU steady (2-5m)
- `login`: reqs=877, avg=107.54ms, p95=162.75ms, fail=0%
- `validate`: reqs=7025, avg=3.48ms, p95=7.72ms, fail=0%
- `register`: reqs=862, avg=106.99ms, p95=161.83ms, fail=0%

### 100 VU steady (7-10m)
- `login`: reqs=1692, avg=117.02ms, p95=196.90ms, fail=0%
- `validate`: reqs=14042, avg=3.61ms, p95=11.09ms, fail=0%
- `register`: reqs=1774, avg=116.87ms, p95=192.77ms, fail=0%

### 200 VU steady (12-15m)
- `login`: reqs=3406, avg=178.43ms, p95=409.16ms, fail=0%
- `validate`: reqs=27371, avg=18.16ms, p95=95.74ms, fail=0%
- `register`: reqs=3426, avg=175.10ms, p95=388.41ms, fail=0%

### 300 VU steady (16-18m)
- `login`: reqs=3175, avg=299.67ms, p95=674.96ms, fail=0%
- `validate`: reqs=25899, avg=68.34ms, p95=270.63ms, fail=0%
- `register`: reqs=3209, avg=298.84ms, p95=683.83ms, fail=0%

## Final Threshold Check (Final Best Secure Run)
- Passed:
  - `login` p95 < 2000ms
  - `validate` p95 < 500ms (actual aggregate p95: `159.58ms`)
  - `register` p95 < 2000ms
  - all error-rate thresholds (0% failures)

## Baseline vs Final Snapshot (Secure Only)
- Baseline secure/DB-only failure case (early abort): `tests/performance/reports/stress-2026-02-09T09-18-37-579Z.json`
  - `http_reqs`: 344
  - `checks`: 98.29%
  - p95 (login/validate/register): 17726ms / 10408ms / 15436ms
  - fail rates (login/validate/register): 4.62% / 2.06% / 0%

- Final best secure/DB-only run: `tests/performance/reports/stress-2026-02-09T17-59-46-341Z.json`
  - `http_reqs`: 152,815
  - `checks`: 100%
  - p95 (login/validate/register): 503.90ms / 159.58ms / 506.84ms
  - fail rates (login/validate/register): 0% / 0% / 0%

## Bottleneck Observation Summary
- Initial failures were dominated by connection starvation/timeouts (`HikariPool ... Connection is not available`) and queueing.
- After tuning, those errors disappeared from Auth logs and throughput rose strongly.
- Remaining 300 VU latency is still higher than 10 VU latency, but the gap is now much smaller and all thresholds pass under secure settings.

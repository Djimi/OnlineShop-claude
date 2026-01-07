# Auth Service Performance Testing Plan

> **Date**: 2025-12-31
> **Status**: Planning (Awaiting Approval)
> **Service**: Auth (Spring Boot 4.0 / Java 25)
> **Focus**: Performance Testing (Load Testing, Response Time Measurement)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [What is Performance Testing?](#what-is-performance-testing)
3. [Best Practices](#best-practices)
4. [Technology Comparison & Recommendation](#technology-comparison--recommendation)
5. [How Time is Measured](#how-time-is-measured)
6. [How Automation Works](#how-automation-works)
7. [Implementation Plan for Auth Service](#implementation-plan-for-auth-service)
8. [Appendix: Code Examples](#appendix-code-examples)

---

## Executive Summary

This plan focuses on implementing **performance testing** for the Auth service to measure response times from an end-user perspective. The goal is to:

- Establish **baseline performance metrics** for all Auth endpoints
- Detect **performance regressions** automatically in CI/CD
- Understand **system behavior under load** (capacity planning)
- Set and validate **Service Level Objectives (SLOs)**

**Recommended Approach**: **k6** (JavaScript-based, lightweight, CI/CD native)

---

## What is Performance Testing?

Performance testing is an umbrella term covering several testing types:

### Types of Performance Tests

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                      PERFORMANCE TESTING TYPES                               │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐        │
│  │   SMOKE     │  │    LOAD     │  │   STRESS    │  │    SOAK     │        │
│  │   TEST      │  │    TEST     │  │    TEST     │  │    TEST     │        │
│  ├─────────────┤  ├─────────────┤  ├─────────────┤  ├─────────────┤        │
│  │ Minimal     │  │ Expected    │  │ Beyond      │  │ Extended    │        │
│  │ load        │  │ load        │  │ capacity    │  │ duration    │        │
│  │             │  │             │  │             │  │             │        │
│  │ Purpose:    │  │ Purpose:    │  │ Purpose:    │  │ Purpose:    │        │
│  │ Sanity      │  │ Baseline &  │  │ Find        │  │ Memory      │        │
│  │ check       │  │ SLO valid.  │  │ limits      │  │ leaks       │        │
│  └─────────────┘  └─────────────┘  └─────────────┘  └─────────────┘        │
│                                                                              │
│  ┌─────────────┐  ┌─────────────┐                                          │
│  │   SPIKE     │  │  BREAKPOINT │                                          │
│  │   TEST      │  │    TEST     │                                          │
│  ├─────────────┤  ├─────────────┤                                          │
│  │ Sudden      │  │ Incremental │                                          │
│  │ traffic     │  │ increase    │                                          │
│  │ bursts      │  │ until fail  │                                          │
│  │             │  │             │                                          │
│  │ Purpose:    │  │ Purpose:    │                                          │
│  │ Auto-scale  │  │ Max         │                                          │
│  │ validation  │  │ capacity    │                                          │
│  └─────────────┘  └─────────────┘                                          │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Detailed Descriptions

| Test Type | Duration | Load Pattern | Purpose | When to Use |
|-----------|----------|--------------|---------|-------------|
| **Smoke** | 1-2 min | 1-5 VUs | Sanity check, verify scripts work | Every PR, every build |
| **Load** | 5-30 min | Expected users (ramp up → steady → ramp down) | Baseline metrics, SLO validation | Nightly, pre-release |
| **Stress** | 20-60 min | Beyond expected (150-200%) | Find breaking points, error handling | Weekly, capacity planning |
| **Soak** | 2-8+ hours | Steady expected load | Memory leaks, connection exhaustion | Weekly, before major releases |
| **Spike** | 10-20 min | Sudden bursts (10x normal) | Auto-scaling, recovery behavior | When using auto-scaling infra |
| **Breakpoint** | Variable | Incremental until failure | Maximum capacity | Capacity planning |

### Auth Service Test Scenarios

| Endpoint | Smoke | Load | Stress | Soak |
|----------|-------|------|--------|------|
| `POST /api/v1/auth/register` | 1 VU, 10 req | 50 VU, 5 min | 200 VU | 50 VU, 2h |
| `POST /api/v1/auth/login` | 1 VU, 10 req | 100 VU, 5 min | 500 VU | 100 VU, 2h |
| `GET /api/v1/auth/validate` | 1 VU, 10 req | 200 VU, 5 min | 1000 VU | 200 VU, 2h |

---

## Best Practices

### 1. Test Pyramid for Performance

```
                    ┌─────────────┐
                    │  Breakpoint │  ← Quarterly
                    │   & Soak    │
                ┌───┴─────────────┴───┐
                │    Stress Tests     │  ← Weekly
            ┌───┴─────────────────────┴───┐
            │       Load Tests            │  ← Nightly / Pre-release
        ┌───┴─────────────────────────────┴───┐
        │           Smoke Tests               │  ← Every PR / Commit
        └─────────────────────────────────────┘
```

### 2. Define Clear Objectives (SLOs)

Before testing, define what "good performance" means:

```yaml
# Proposed SLOs for Auth Service
auth_service_slos:
  register:
    p95_response_time: 500ms    # BCrypt is CPU-intensive
    p99_response_time: 800ms
    error_rate: < 0.1%

  login:
    p95_response_time: 300ms
    p99_response_time: 500ms
    error_rate: < 0.1%

  validate:
    p95_response_time: 50ms     # Critical path - must be fast
    p99_response_time: 100ms
    error_rate: < 0.01%
```

### 3. Tiered Testing Strategy

**Tier 1 - Smoke Tests (Every PR)**
- Lightweight, fast (< 2 minutes)
- Catch catastrophic regressions
- Run against every pull request

**Tier 2 - Load Tests (Nightly / Pre-release)**
- Realistic load patterns
- Compare against baselines
- Flag regressions even within thresholds

**Tier 3 - Full Tests (Weekly / Pre-major release)**
- Stress, soak, spike tests
- Find limits and edge cases
- Capacity planning

### 4. Realistic Test Scenarios

```
❌ BAD: All requests hit /login continuously
✅ GOOD: Mix of operations reflecting real usage

Real Traffic Pattern (example):
├── 60% Token validation (validate)
├── 30% Login attempts (login)
└── 10% New registrations (register)
```

### 5. Use Percentiles, Not Averages

```
WHY PERCENTILES MATTER:

Scenario: 1000 requests
- 990 requests: 50ms
- 10 requests: 5000ms (slow!)

Average: (990×50 + 10×5000) / 1000 = 99.5ms  ← Looks fine!
P99: 5000ms  ← Reveals the problem!

┌────────────────────────────────────────────────────────────┐
│ Average can hide tail latency issues that affect real users │
│ P99 shows what your slowest 1% of users experience          │
└────────────────────────────────────────────────────────────┘
```

### 6. Establish Baselines First

```
1. Run initial tests → Record baseline metrics
2. Store baselines in version control
3. Compare future runs against baselines
4. Alert on regression (e.g., >10% slower than baseline)
```

### 7. Isolate Test Environment

- Use dedicated test environment (not production!)
- Reset database state between test runs
- Control external dependencies (mock if needed)
- Ensure consistent infrastructure

### 8. Think Time & Realistic Pacing

```javascript
// ❌ BAD: No think time (unrealistic, hammers server)
export default function () {
    http.get('/api/v1/auth/validate');
    http.get('/api/v1/auth/validate');
    http.get('/api/v1/auth/validate');
}

// ✅ GOOD: Realistic think time between requests
export default function () {
    http.get('/api/v1/auth/validate');
    sleep(randomIntBetween(1, 3));  // User thinks for 1-3 seconds
    http.get('/api/v1/auth/validate');
}
```

---

## Technology Comparison & Recommendation

### Tool Comparison Matrix

| Feature | k6 | Gatling | JMeter | Locust |
|---------|-----|---------|--------|--------|
| **Language** | JavaScript | Scala/Java/Kotlin | XML/GUI | Python |
| **Learning Curve** | Easy (JS devs) | Medium | Easy (GUI) | Easy (Python devs) |
| **Resource Usage** | Very Low | Low | High | Medium |
| **CI/CD Integration** | Excellent | Excellent | Good | Good |
| **Reporting** | CLI + Grafana | Excellent HTML | Plugins needed | Web UI |
| **Scripting** | Code-first | Code-first | GUI-first | Code-first |
| **Protocol Support** | HTTP, WebSocket, gRPC | HTTP, WebSocket | 30+ protocols | HTTP, custom |
| **Cloud Option** | Grafana Cloud k6 | Gatling Enterprise | BlazeMeter | Locust Cloud |
| **License** | AGPL-3.0 | Apache 2.0 | Apache 2.0 | MIT |
| **Distributed Testing** | Via K8s/Cloud | Built-in | Built-in | Built-in |

### Detailed Analysis

#### k6 (Recommended)

**Why k6 for Auth Service:**
1. **JavaScript** - Familiar to most developers
2. **Lightweight** - Single binary, low resource usage
3. **CI/CD Native** - Official GitHub Actions, GitLab CI support
4. **Grafana Ecosystem** - Easy visualization with Grafana dashboards
5. **Thresholds** - Built-in pass/fail criteria for automation
6. **Modern** - Built for cloud-native, microservices architectures

**Limitations:**
- AGPL license (copyleft - not an issue for testing)
- No native distributed mode (needs K8s or cloud)
- HTTP-focused (limited protocol support)

```javascript
// k6 example - clean, readable syntax
import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
    vus: 100,
    duration: '5m',
    thresholds: {
        http_req_duration: ['p(95)<300'],
    },
};

export default function () {
    const res = http.post('http://localhost:9001/api/v1/auth/login',
        JSON.stringify({ username: 'test', password: 'pass' }),
        { headers: { 'Content-Type': 'application/json' } }
    );
    check(res, { 'login successful': (r) => r.status === 200 });
    sleep(1);
}
```

#### Gatling

**Why Consider Gatling:**
1. **Java/Scala** - Same language as Auth service
2. **Excellent Reports** - Beautiful HTML reports out of box
3. **Apache 2.0** - Permissive license
4. **High Performance** - Async, non-blocking architecture

**Limitations:**
- Scala DSL learning curve
- Enterprise features are paid

```scala
// Gatling example - Scala DSL
class AuthSimulation extends Simulation {
  val httpProtocol = http.baseUrl("http://localhost:9001")

  val scn = scenario("Login Test")
    .exec(http("login")
      .post("/api/v1/auth/login")
      .body(StringBody("""{"username":"test","password":"pass"}"""))
      .check(status.is(200)))
    .pause(1)

  setUp(scn.inject(rampUsers(100).during(60))).protocols(httpProtocol)
}
```

#### JMeter

**Why Consider JMeter:**
1. **GUI** - Visual test creation (good for non-developers)
2. **Protocol Support** - JDBC, JMS, FTP, LDAP, etc.
3. **Mature** - 25+ years, huge community
4. **Portable** - Scripts run anywhere with BlazeMeter, etc.

**Limitations:**
- Resource-heavy (JVM-based)
- XML format difficult to version control
- GUI can be slow for large tests

### Recommendation Decision Matrix

| Criteria | Weight | k6 | Gatling | JMeter |
|----------|--------|-----|---------|--------|
| Developer experience | 25% | 9 | 7 | 5 |
| CI/CD integration | 25% | 10 | 8 | 6 |
| Resource efficiency | 15% | 10 | 8 | 4 |
| Reporting quality | 15% | 7 | 10 | 6 |
| Learning curve | 10% | 9 | 6 | 7 |
| Community/Support | 10% | 8 | 8 | 10 |
| **Weighted Score** | | **8.85** | **7.75** | **5.95** |

### Final Recommendation: **k6**

```
┌─────────────────────────────────────────────────────────────────────┐
│                     RECOMMENDATION: k6                               │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  Primary Reasons:                                                    │
│  1. JavaScript - familiar syntax, easy to maintain                   │
│  2. CI/CD native - official GitHub Actions support                   │
│  3. Lightweight - can run in CI without resource issues              │
│  4. Thresholds - built-in pass/fail for automated pipelines          │
│  5. Grafana integration - visualize alongside app metrics            │
│                                                                      │
│  Alternative: Gatling (if team prefers Java/Scala, needs better      │
│               built-in reports)                                      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

---

## How Time is Measured

### HTTP Request Timing Breakdown

When you make an HTTP request, multiple phases contribute to total time:

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                        HTTP REQUEST TIMELINE                                 │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  ┌──────┐ ┌─────────┐ ┌─────────┐ ┌───────┐ ┌───────┐ ┌──────────┐         │
│  │ DNS  │ │   TCP   │ │   TLS   │ │ Send  │ │ Wait  │ │ Receive  │         │
│  │Lookup│ │ Connect │ │Handshake│ │Request│ │(TTFB) │ │ Response │         │
│  └──┬───┘ └────┬────┘ └────┬────┘ └───┬───┘ └───┬───┘ └─────┬────┘         │
│     │          │           │          │         │           │               │
│  ◄──┴──────────┴───────────┴──────────┴─────────┴───────────┴──►            │
│                                                                              │
│  │◄─────── Connection Setup ────────►│◄───── Request/Response ─────►│       │
│  │         (one-time per conn)       │       (per request)          │       │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### k6 Timing Metrics

k6 provides granular timing breakdown:

| k6 Metric | Description | Typical Value |
|-----------|-------------|---------------|
| `http_req_blocked` | Time waiting for free connection slot | 0-10ms |
| `http_req_connecting` | TCP connection establishment | 1-50ms |
| `http_req_tls_handshaking` | TLS/SSL handshake | 10-100ms |
| `http_req_sending` | Time to send request body | 0-5ms |
| `http_req_waiting` | **Time to First Byte (TTFB)** - server processing | 10-500ms |
| `http_req_receiving` | Time to receive response body | 1-50ms |
| `http_req_duration` | **sending + waiting + receiving** | Main metric |

### What k6 Reports

```
http_req_duration formula:
┌─────────────────────────────────────────────────────────────────┐
│ http_req_duration = http_req_sending                            │
│                   + http_req_waiting    ← Server processing     │
│                   + http_req_receiving                          │
│                                                                  │
│ NOTE: Does NOT include DNS, TCP connect, or TLS handshake       │
│       (those happen once per connection with keep-alive)        │
└─────────────────────────────────────────────────────────────────┘
```

### Percentile Calculation

```
How P95 is calculated:

1. Collect all response times: [23, 45, 67, 89, 102, 156, 234, 345, 456, 1234]
2. Sort ascending: [23, 45, 67, 89, 102, 156, 234, 345, 456, 1234]
3. Find 95th percentile position: ceil(0.95 × 10) = 10
4. P95 = 1234ms (the 10th value)

Interpretation: 95% of requests completed in ≤1234ms

┌─────────────────────────────────────────────────────────────────┐
│                    PERCENTILE MEANINGS                           │
├─────────────────────────────────────────────────────────────────┤
│  P50 (median) : Typical user experience                         │
│  P90          : What 90% of users experience or better          │
│  P95          : Industry standard for SLO definition            │
│  P99          : Tail latency - your worst 1% of users           │
│  P99.9        : Extreme outliers (important for high traffic)   │
└─────────────────────────────────────────────────────────────────┘
```

### Real-World Example

```
Auth Service /login endpoint under load:

┌────────────────────────────────────────────────────────────────┐
│ Response Time Distribution (10,000 requests)                   │
├────────────────────────────────────────────────────────────────┤
│                                                                │
│  0-50ms    ████████████████████████████████████  3,200 (32%)   │
│  50-100ms  ██████████████████████████████████████ 3,800 (38%)  │
│  100-200ms ████████████████████  1,800 (18%)                   │
│  200-300ms ████████  800 (8%)                                  │
│  300-500ms ███  300 (3%)                                       │
│  500ms+    █  100 (1%)                                         │
│                                                                │
├────────────────────────────────────────────────────────────────┤
│  Statistics:                                                   │
│  ├── Min:     12ms                                             │
│  ├── Max:     2,341ms                                          │
│  ├── Average: 98ms    ← Can be misleading!                     │
│  ├── P50:     72ms    ← Median, typical experience             │
│  ├── P90:     189ms                                            │
│  ├── P95:     267ms   ← SLO target                             │
│  └── P99:     489ms   ← Tail latency                           │
└────────────────────────────────────────────────────────────────┘
```

### Timing in k6 Code

```javascript
import http from 'k6/http';
import { Trend } from 'k6/metrics';

// Custom metric to track login specifically
const loginDuration = new Trend('auth_login_duration');

export default function () {
    const res = http.post('http://localhost:9001/api/v1/auth/login',
        JSON.stringify({ username: 'test', password: 'test123' }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    // Access individual timing phases
    console.log(`TTFB (waiting): ${res.timings.waiting}ms`);
    console.log(`Total duration: ${res.timings.duration}ms`);

    // Record to custom metric
    loginDuration.add(res.timings.duration);
}
```

---

## How Automation Works

### CI/CD Integration Architecture

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                    PERFORMANCE TESTING AUTOMATION                            │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│  Developer                     CI/CD Pipeline                                │
│  ─────────                     ──────────────                                │
│                                                                              │
│  ┌──────────┐    push     ┌────────────────────────────────────────────┐   │
│  │   Code   │────────────►│  1. Build Application                      │   │
│  │  Change  │             │  2. Run Unit Tests                         │   │
│  └──────────┘             │  3. Deploy to Test Environment             │   │
│                           │  4. Run Smoke Tests (k6)          ◄────────────┤   │
│                           │     └─► Pass/Fail gate                     │   │
│                           │  5. [On merge] Run Load Tests              │   │
│                           │     └─► Compare to baseline                │   │
│                           │     └─► Store results                      │   │
│                           │  6. Generate Report                        │   │
│                           └────────────────────────────────────────────┘   │
│                                          │                                   │
│                                          ▼                                   │
│                           ┌────────────────────────────────────────────┐   │
│                           │           RESULTS & ALERTS                  │   │
│                           │  ┌─────────────┐  ┌─────────────────────┐  │   │
│                           │  │  Grafana    │  │  Slack/Teams Alert  │  │   │
│                           │  │  Dashboard  │  │  if thresholds fail │  │   │
│                           │  └─────────────┘  └─────────────────────┘  │   │
│                           └────────────────────────────────────────────┘   │
│                                                                              │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Threshold-Based Pass/Fail

k6 thresholds enable automatic pass/fail:

```javascript
export const options = {
    thresholds: {
        // 95% of requests must complete under 300ms
        'http_req_duration': ['p(95)<300'],

        // 99% of requests must complete under 500ms
        'http_req_duration': ['p(99)<500'],

        // Error rate must be below 1%
        'http_req_failed': ['rate<0.01'],

        // Custom metric thresholds
        'auth_login_duration': ['p(95)<300', 'p(99)<500'],
        'auth_validate_duration': ['p(95)<50', 'p(99)<100'],
    },
};
```

### GitHub Actions Integration

```yaml
# .github/workflows/performance.yml
name: Performance Tests

on:
  pull_request:
    branches: [main]
  push:
    branches: [main]
  schedule:
    - cron: '0 2 * * *'  # Nightly at 2 AM

jobs:
  smoke-test:
    runs-on: ubuntu-latest
    if: github.event_name == 'pull_request'
    steps:
      - uses: actions/checkout@v4

      - name: Start Auth Service
        run: |
          docker compose up -d auth-service
          sleep 30

      - name: Run Smoke Tests
        uses: grafana/k6-action@v0.3.1
        with:
          filename: tests/performance/smoke.js
          flags: --out json=smoke-results.json

      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: smoke-results
          path: smoke-results.json

  load-test:
    runs-on: ubuntu-latest
    if: github.ref == 'refs/heads/main'
    steps:
      - uses: actions/checkout@v4

      - name: Start Auth Service
        run: |
          docker compose up -d auth-service
          sleep 30

      - name: Run Load Tests
        uses: grafana/k6-action@v0.3.1
        with:
          filename: tests/performance/load.js
          flags: --out json=load-results.json

      - name: Compare with Baseline
        run: |
          # Custom script to compare results with stored baseline
          python scripts/compare-baseline.py load-results.json

      - name: Store New Baseline
        run: |
          cp load-results.json baselines/latest.json
          git config user.name "GitHub Actions"
          git config user.email "actions@github.com"
          git add baselines/
          git commit -m "Update performance baseline" || true
          git push || true
```

### Baseline Comparison Logic

```javascript
// scripts/compare-baseline.js
const baseline = require('./baselines/latest.json');
const current = require('./results/current.json');

const REGRESSION_THRESHOLD = 0.10; // 10% regression allowed

function checkRegression(metric, baselineValue, currentValue) {
    const change = (currentValue - baselineValue) / baselineValue;
    if (change > REGRESSION_THRESHOLD) {
        console.error(`REGRESSION: ${metric} increased by ${(change * 100).toFixed(1)}%`);
        console.error(`  Baseline: ${baselineValue}ms`);
        console.error(`  Current:  ${currentValue}ms`);
        return false;
    }
    return true;
}

// Compare P95 values
const passed = checkRegression('login_p95',
    baseline.metrics.auth_login_duration.p95,
    current.metrics.auth_login_duration.p95
);

process.exit(passed ? 0 : 1);
```

### Scheduled Test Runs

```
┌─────────────────────────────────────────────────────────────────┐
│                    TEST SCHEDULE                                 │
├─────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Frequency        Test Type       Environment    Duration        │
│  ─────────        ─────────       ───────────    ────────        │
│  Every PR         Smoke           CI ephemeral   ~2 min          │
│  On merge         Load            Staging        ~10 min         │
│  Nightly (2 AM)   Full Load       Staging        ~30 min         │
│  Weekly (Sun)     Stress + Soak   Staging        ~4 hours        │
│  Pre-release      All types       Pre-prod       ~8 hours        │
│                                                                  │
└─────────────────────────────────────────────────────────────────┘
```

---

## Implementation Plan for Auth Service

### Phase 1: Setup & Smoke Tests (Week 1)

**Tasks:**
- [ ] Install k6 locally and in CI
- [ ] Create project structure for performance tests
- [ ] Write smoke tests for all 3 endpoints
- [ ] Add GitHub Actions workflow for smoke tests on PRs
- [ ] Document test execution process

**Deliverables:**
```
tests/
└── performance/
    ├── smoke.js           # Quick sanity check
    ├── config/
    │   └── thresholds.js  # SLO definitions
    └── utils/
        └── auth-helpers.js # Login/token helpers
```

### Phase 2: Load Tests & Baselines (Week 2)

**Tasks:**
- [ ] Write comprehensive load test scenarios
- [ ] Run initial baseline tests
- [ ] Store baselines in repository
- [ ] Add load tests to nightly CI schedule
- [ ] Create baseline comparison script

**Deliverables:**
```
tests/
└── performance/
    ├── smoke.js
    ├── load.js            # Full load test
    ├── scenarios/
    │   ├── login-flow.js
    │   ├── validate-flow.js
    │   └── mixed-traffic.js
    └── baselines/
        └── latest.json    # Stored baseline
```

### Phase 3: Advanced Tests & Reporting (Week 3)

**Tasks:**
- [ ] Write stress test scenarios
- [ ] Write soak test scenarios
- [ ] Set up Grafana dashboard for results visualization
- [ ] Add Slack/Teams notifications for failures
- [ ] Document all test scenarios and thresholds

**Deliverables:**
```
tests/
└── performance/
    ├── smoke.js
    ├── load.js
    ├── stress.js          # Find breaking points
    ├── soak.js            # Long-running stability
    └── dashboards/
        └── grafana-auth-perf.json
```

### Phase 4: Full Automation & Documentation (Week 4)

**Tasks:**
- [ ] Complete CI/CD integration for all test types
- [ ] Set up scheduled test runs (nightly, weekly)
- [ ] Create runbooks for test execution
- [ ] Train team on test maintenance
- [ ] Establish regression detection alerts

### File Structure

```
OnlineShop-claude/
├── Auth/
│   └── ... (existing service code)
└── tests/
    └── performance/
        ├── README.md                    # How to run tests
        ├── package.json                 # k6 dependencies (if using extensions)
        │
        ├── smoke.js                     # PR-level tests
        ├── load.js                      # Nightly load tests
        ├── stress.js                    # Weekly stress tests
        ├── soak.js                      # Pre-release soak tests
        │
        ├── scenarios/
        │   ├── auth-register.js         # Registration flow
        │   ├── auth-login.js            # Login flow
        │   ├── auth-validate.js         # Token validation
        │   └── auth-mixed.js            # Realistic traffic mix
        │
        ├── config/
        │   ├── thresholds.js            # SLO definitions
        │   ├── environments.js          # URL configs per env
        │   └── test-users.json          # Test data
        │
        ├── utils/
        │   ├── helpers.js               # Common functions
        │   └── metrics.js               # Custom metrics
        │
        ├── baselines/
        │   ├── smoke-baseline.json
        │   ├── load-baseline.json
        │   └── README.md                # Baseline update process
        │
        └── reports/
            └── .gitkeep                 # Generated reports go here
```

---

## Appendix: Code Examples

### Complete Smoke Test

```javascript
// tests/performance/smoke.js
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('auth_login_duration');
const validateDuration = new Trend('auth_validate_duration');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9001';

export const options = {
    vus: 1,
    iterations: 10,
    thresholds: {
        'http_req_duration': ['p(95)<1000'],  // Generous for smoke
        'errors': ['rate<0.1'],
    },
};

export default function () {
    group('Auth Smoke Tests', function () {
        // Test 1: Login
        group('Login', function () {
            const loginRes = http.post(
                `${BASE_URL}/api/v1/auth/login`,
                JSON.stringify({
                    username: 'testuser',
                    password: 'testpass123',
                }),
                { headers: { 'Content-Type': 'application/json' } }
            );

            const loginSuccess = check(loginRes, {
                'login status is 200': (r) => r.status === 200,
                'login has token': (r) => {
                    try {
                        return JSON.parse(r.body).token !== undefined;
                    } catch {
                        return false;
                    }
                },
            });

            errorRate.add(!loginSuccess);
            loginDuration.add(loginRes.timings.duration);

            // Test 2: Validate token (if login succeeded)
            if (loginSuccess && loginRes.status === 200) {
                const token = JSON.parse(loginRes.body).token;

                const validateRes = http.get(
                    `${BASE_URL}/api/v1/auth/validate`,
                    { headers: { 'Authorization': `Bearer ${token}` } }
                );

                const validateSuccess = check(validateRes, {
                    'validate status is 200': (r) => r.status === 200,
                });

                errorRate.add(!validateSuccess);
                validateDuration.add(validateRes.timings.duration);
            }
        });
    });

    sleep(1);
}
```

### Complete Load Test

```javascript
// tests/performance/load.js
import http from 'k6/http';
import { check, group, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// Custom metrics
const errorRate = new Rate('errors');
const loginDuration = new Trend('auth_login_duration');
const validateDuration = new Trend('auth_validate_duration');
const registerDuration = new Trend('auth_register_duration');
const successfulLogins = new Counter('successful_logins');
const failedLogins = new Counter('failed_logins');

// Configuration
const BASE_URL = __ENV.BASE_URL || 'http://localhost:9001';

export const options = {
    scenarios: {
        // Ramp up, steady state, ramp down
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 50 },   // Ramp up
                { duration: '3m', target: 50 },   // Steady state
                { duration: '1m', target: 100 },  // Peak
                { duration: '2m', target: 100 },  // Sustained peak
                { duration: '1m', target: 0 },    // Ramp down
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: {
        // SLOs
        'auth_login_duration': ['p(95)<300', 'p(99)<500'],
        'auth_validate_duration': ['p(95)<50', 'p(99)<100'],
        'auth_register_duration': ['p(95)<500', 'p(99)<800'],
        'errors': ['rate<0.01'],  // Less than 1% errors
        'http_req_duration': ['p(95)<500'],
    },
};

// Test data - unique per VU
function getTestUser() {
    return {
        username: `loadtest_user_${__VU}_${__ITER}`,
        password: 'LoadTest123!',
    };
}

export default function () {
    const user = getTestUser();

    // Simulate realistic traffic mix: 60% validate, 30% login, 10% register
    const action = Math.random();

    if (action < 0.10) {
        // 10% - Registration
        testRegister(user);
    } else if (action < 0.40) {
        // 30% - Login
        testLogin(user);
    } else {
        // 60% - Validate (most common in real usage)
        testValidate();
    }

    // Think time - realistic user behavior
    sleep(randomIntBetween(1, 3));
}

function testRegister(user) {
    group('Register', function () {
        const res = http.post(
            `${BASE_URL}/api/v1/auth/register`,
            JSON.stringify({
                username: user.username,
                password: user.password,
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        const success = check(res, {
            'register status is 201 or 409': (r) => r.status === 201 || r.status === 409,
        });

        errorRate.add(!success);
        registerDuration.add(res.timings.duration);
    });
}

function testLogin(user) {
    group('Login', function () {
        const res = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                username: 'testuser',  // Use known test user
                password: 'testpass123',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        const success = check(res, {
            'login status is 200': (r) => r.status === 200,
            'login response has token': (r) => {
                try {
                    return JSON.parse(r.body).token !== undefined;
                } catch {
                    return false;
                }
            },
        });

        if (success) {
            successfulLogins.add(1);
        } else {
            failedLogins.add(1);
        }

        errorRate.add(!success);
        loginDuration.add(res.timings.duration);
    });
}

function testValidate() {
    group('Validate', function () {
        // First login to get a token
        const loginRes = http.post(
            `${BASE_URL}/api/v1/auth/login`,
            JSON.stringify({
                username: 'testuser',
                password: 'testpass123',
            }),
            { headers: { 'Content-Type': 'application/json' } }
        );

        if (loginRes.status === 200) {
            const token = JSON.parse(loginRes.body).token;

            const validateRes = http.get(
                `${BASE_URL}/api/v1/auth/validate`,
                { headers: { 'Authorization': `Bearer ${token}` } }
            );

            const success = check(validateRes, {
                'validate status is 200': (r) => r.status === 200,
            });

            errorRate.add(!success);
            validateDuration.add(validateRes.timings.duration);
        }
    });
}

export function handleSummary(data) {
    return {
        'reports/load-summary.json': JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
```

### Stress Test

```javascript
// tests/performance/stress.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

const errorRate = new Rate('errors');
const loginDuration = new Trend('auth_login_duration');

const BASE_URL = __ENV.BASE_URL || 'http://localhost:9001';

export const options = {
    scenarios: {
        stress: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '2m', target: 100 },   // Below normal
                { duration: '5m', target: 100 },   // Normal load
                { duration: '2m', target: 200 },   // Around breaking point
                { duration: '5m', target: 200 },   // Stay at breaking point
                { duration: '2m', target: 300 },   // Beyond breaking point
                { duration: '5m', target: 300 },   // Stay beyond
                { duration: '5m', target: 0 },     // Recovery
            ],
            gracefulRampDown: '2m',
        },
    },
    thresholds: {
        // More lenient - we expect some failures under stress
        'http_req_duration': ['p(95)<2000'],
        'errors': ['rate<0.30'],  // Allow up to 30% errors under extreme stress
    },
};

export default function () {
    const res = http.post(
        `${BASE_URL}/api/v1/auth/login`,
        JSON.stringify({
            username: 'testuser',
            password: 'testpass123',
        }),
        { headers: { 'Content-Type': 'application/json' } }
    );

    const success = check(res, {
        'status is 200': (r) => r.status === 200,
    });

    errorRate.add(!success);
    loginDuration.add(res.timings.duration);

    sleep(1);
}
```

---

## Decision Points for Discussion

Before proceeding with implementation, please confirm:

1. **Tool Selection**: k6 (recommended) vs Gatling vs JMeter?

2. **Test Environment**:
   - Run against local Docker environment?
   - Set up dedicated staging environment?
   - Use existing infrastructure?

3. **SLO Targets**: Are these acceptable?
   - Login: p95 < 300ms, p99 < 500ms
   - Validate: p95 < 50ms, p99 < 100ms
   - Register: p95 < 500ms, p99 < 800ms

4. **Test Data Strategy**:
   - Pre-seed test users in database?
   - Create users dynamically during tests?
   - Use shared test account?

5. **CI/CD Integration Priority**:
   - Start with manual execution?
   - Immediately integrate into GitHub Actions?

6. **Reporting Preferences**:
   - CLI output sufficient for now?
   - Set up Grafana dashboards from start?

---

## Sources

- [k6 Official Documentation](https://k6.io/docs/)
- [Grafana k6 Testing Guides](https://grafana.com/docs/k6/latest/testing-guides/)
- [k6 GitHub Actions Integration](https://grafana.com/blog/2024/07/15/performance-testing-with-grafana-k6-and-github-actions/)
- [Performance Testing Types - Grafana](https://grafana.com/load-testing/types-of-load-testing/)
- [k6 Built-in Metrics](https://grafana.com/docs/k6/latest/using-k6/metrics/reference/)
- [JMeter vs Gatling vs k6 Comparison](https://codoid.com/latest-post/jmeter-vs-gatling-vs-k6-comparing-top-performance-testing-tools/)
- [Performance Testing Metrics - BlazeMeter](https://www.blazemeter.com/blog/key-test-metrics-to-track)
- [CI/CD Performance Testing Best Practices](https://devops.com/integrating-performance-testing-into-ci-cd-a-practical-framework/)

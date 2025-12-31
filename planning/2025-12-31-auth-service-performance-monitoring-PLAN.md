# Auth Service Performance Monitoring Plan

> **Date**: 2025-12-31
> **Status**: Research & Planning
> **Service**: Auth (Spring Boot 4.0 / Java 25)

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Current State Analysis](#current-state-analysis)
3. [Best Practices for Service Performance Monitoring](#best-practices)
4. [Technology Options & Recommendations](#technology-options)
5. [How Time Is Measured](#how-time-is-measured)
6. [Automation Strategies](#automation-strategies)
7. [Implementation Roadmap](#implementation-roadmap)
8. [Architecture Diagrams](#architecture-diagrams)

---

## Executive Summary

This document outlines a comprehensive approach to monitoring the performance of the Auth service. The goal is to gain visibility into:
- **Response times** (latency percentiles: p50, p95, p99)
- **Throughput** (requests/second)
- **Error rates** (4xx, 5xx responses)
- **Resource utilization** (CPU, memory, DB connections)
- **Business metrics** (login success/failure rates, token validations/second)

---

## Current State Analysis

### What Auth Service Currently Has

| Component | Status | Notes |
|-----------|--------|-------|
| Spring Boot Actuator | ✅ Enabled | Only `health`, `env`, `configprops` exposed |
| Liveness/Readiness probes | ✅ Enabled | Kubernetes-ready |
| Micrometer metrics | ❌ Not configured | No `/actuator/metrics` or `/actuator/prometheus` |
| Distributed tracing | ❌ Not configured | No trace correlation |
| Custom business metrics | ❌ Not configured | No login/validation tracking |
| Performance testing | ❌ Not configured | No load testing suite |
| Continuous profiling | ❌ Not configured | No CPU/memory profiling |

### Key Endpoints to Monitor

| Endpoint | Method | Expected Behavior |
|----------|--------|-------------------|
| `/api/v1/auth/register` | POST | < 500ms p99 (includes BCrypt hashing) |
| `/api/v1/auth/login` | POST | < 300ms p99 |
| `/api/v1/auth/validate` | GET | < 50ms p99 (critical path) |

---

## Best Practices

### 1. The Three Pillars of Observability

Modern observability is built on three pillars that work together:

```
┌─────────────────────────────────────────────────────────────┐
│                    OBSERVABILITY                            │
├───────────────────┬───────────────────┬────────────────────┤
│      METRICS      │      TRACES       │       LOGS         │
├───────────────────┼───────────────────┼────────────────────┤
│ - Request count   │ - Request flow    │ - Error details    │
│ - Latency (p99)   │ - Service deps    │ - Audit events     │
│ - Error rate      │ - Bottlenecks     │ - Debug info       │
│ - Saturation      │ - Latency source  │ - Context          │
└───────────────────┴───────────────────┴────────────────────┘
```

### 2. RED Method (Rate, Errors, Duration)

For request-driven services like Auth, focus on:

| Metric | Description | Auth Service Example |
|--------|-------------|---------------------|
| **Rate** | Requests per second | `auth.login.requests` |
| **Errors** | Failed requests per second | `auth.login.failures` |
| **Duration** | Time per request (histogram) | `auth.login.duration` |

### 3. USE Method (Utilization, Saturation, Errors)

For resources (CPU, memory, connections):

| Metric | Description | Auth Service Example |
|--------|-------------|---------------------|
| **Utilization** | % resource busy | CPU usage, DB connection pool usage |
| **Saturation** | Queue length | Thread pool queue, DB connection wait |
| **Errors** | Error count | Connection timeouts, pool exhaustion |

### 4. Golden Signals (Google SRE)

| Signal | What to measure |
|--------|-----------------|
| **Latency** | Time to serve requests (separate success/error) |
| **Traffic** | Demand on the system (requests/sec) |
| **Errors** | Rate of failed requests |
| **Saturation** | How "full" the service is |

### 5. Service Level Objectives (SLOs)

Define clear SLOs for Auth service:

```yaml
# Example SLOs for Auth Service
auth_service_slos:
  availability: 99.9%  # 8.76 hours downtime/year
  latency:
    login_p99: 300ms
    validate_p99: 50ms
    register_p99: 500ms
  error_rate: < 0.1%
```

---

## Technology Options

### Option A: Micrometer + Prometheus + Grafana (Recommended for Start)

**Pros:**
- Native Spring Boot integration
- Zero-code instrumentation for HTTP, JPA, connection pools
- Open-source, widely adopted
- Excellent Grafana dashboards available

**Cons:**
- Pull-based (requires network access to service)
- No distributed tracing out-of-box

**Architecture:**
```
┌─────────────┐    scrape    ┌─────────────┐    query    ┌─────────────┐
│ Auth Service│◄─────────────│  Prometheus │◄────────────│   Grafana   │
│ /prometheus │              │  (TSDB)     │             │ (Dashboard) │
└─────────────┘              └─────────────┘             └─────────────┘
```

**Implementation:**
```xml
<!-- pom.xml additions -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>
```

```yaml
# application.yml additions
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,prometheus
  metrics:
    tags:
      application: auth-service
    distribution:
      percentiles-histogram:
        http.server.requests: true
      percentiles:
        http.server.requests: 0.5, 0.95, 0.99
```

---

### Option B: OpenTelemetry (Modern, Future-Proof)

**Pros:**
- Vendor-neutral, CNCF standard
- Unified metrics, traces, and logs
- Auto-instrumentation for 150+ libraries
- Spring Boot 4.0 native starter support

**Cons:**
- More complex setup
- Requires collector infrastructure

**Architecture:**
```
┌─────────────┐   OTLP    ┌─────────────┐         ┌─────────────┐
│ Auth Service│──────────►│    OTel     │────────►│  Prometheus │
│ + OTel Agent│           │  Collector  │         │  Jaeger     │
└─────────────┘           └─────────────┘         │  Grafana    │
                                                  └─────────────┘
```

**Two Integration Approaches:**

#### B1: Java Agent (Zero Code Changes)
```bash
# Start Auth service with OTel agent
java -javaagent:opentelemetry-javaagent.jar \
     -Dotel.service.name=auth-service \
     -Dotel.exporter.otlp.endpoint=http://collector:4317 \
     -jar auth-service.jar
```

#### B2: Spring Boot Starter (More Control)
```xml
<!-- pom.xml -->
<dependency>
    <groupId>io.opentelemetry.instrumentation</groupId>
    <artifactId>opentelemetry-spring-boot-starter</artifactId>
</dependency>
```

```yaml
# application.yml
otel:
  exporter:
    otlp:
      endpoint: http://collector:4317
  resource:
    attributes:
      service.name: auth-service
```

---

### Option C: Commercial APM (Enterprise)

| Tool | Strengths | Pricing Model |
|------|-----------|---------------|
| **Datadog** | Full-stack, excellent UX, AI insights | Per host + ingestion |
| **New Relic** | Strong Java support, code-level insights | Per GB ingested |
| **Dynatrace** | Auto-discovery, AI-powered root cause | Per host |
| **Elastic APM** | Open-source option, ELK integration | Self-hosted or cloud |

---

### Option D: Continuous Profiling with Pyroscope

**Why Continuous Profiling?**
- Metrics tell you WHAT is slow
- Profiling tells you WHY it's slow (which code path)

**Architecture:**
```
┌─────────────┐   async-profiler   ┌─────────────┐         ┌─────────────┐
│ Auth Service│───────────────────►│  Pyroscope  │────────►│   Grafana   │
│ + Pyroscope │   (2-5% overhead)  │   Server    │         │ (Flame UI)  │
└─────────────┘                    └─────────────┘         └─────────────┘
```

**Implementation:**
```java
// Add Pyroscope agent
java -javaagent:pyroscope.jar \
     -Dpyroscope.application.name=auth-service \
     -Dpyroscope.server.address=http://pyroscope:4040 \
     -jar auth-service.jar
```

---

### Technology Comparison Matrix

| Feature | Micrometer+Prometheus | OpenTelemetry | Commercial APM |
|---------|----------------------|---------------|----------------|
| **Setup Complexity** | Low | Medium | Low |
| **Cost** | Free | Free | $$$-$$$$ |
| **Metrics** | ✅ Excellent | ✅ Good | ✅ Excellent |
| **Tracing** | ❌ Separate | ✅ Built-in | ✅ Built-in |
| **Logs Correlation** | ❌ Manual | ✅ Built-in | ✅ Built-in |
| **Auto-instrumentation** | Partial | Full (150+ libs) | Full |
| **Profiling** | ❌ No | ❌ No | ✅ Some |
| **Alerting** | Via Grafana | Via backends | ✅ Built-in |
| **Spring Boot Support** | Native | Native (4.0+) | Agents |

---

## How Time Is Measured

### 1. Micrometer Timer Mechanics

Micrometer's `Timer` measures both:
- **Count**: Number of events
- **Total time**: Sum of all durations
- **Max**: Maximum observed duration
- **Percentiles**: Distribution (p50, p95, p99)

```java
// How Spring Boot auto-instruments HTTP requests
@Timed(value = "http.server.requests", percentiles = {0.5, 0.95, 0.99})
public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
    // Method execution is timed automatically
}
```

**Under the hood:**
```java
Timer timer = Timer.builder("auth.login.duration")
    .publishPercentiles(0.5, 0.95, 0.99)
    .publishPercentileHistogram()
    .serviceLevelObjectives(Duration.ofMillis(300))
    .register(meterRegistry);

// Measuring
timer.record(() -> authService.login(request));

// Or manually
Timer.Sample sample = Timer.start(meterRegistry);
try {
    return authService.login(request);
} finally {
    sample.stop(timer);
}
```

### 2. Percentile Calculation

| Percentile | Meaning | Use Case |
|------------|---------|----------|
| **p50** (median) | 50% of requests faster | Typical user experience |
| **p95** | 95% of requests faster | Performance tuning target |
| **p99** | 99% of requests faster | Tail latency, SLO definition |
| **p99.9** | 99.9% of requests faster | Extreme outliers |

**Example Distribution:**
```
Latency Distribution for /api/v1/auth/login (1000 requests)
─────────────────────────────────────────────────────────

  0-50ms   │████████████████████████████████████ 720 (72%)
 50-100ms  │████████████ 180 (18%)
100-200ms  │████ 60 (6%)
200-300ms  │██ 30 (3%)
300-500ms  │█ 8 (0.8%)
  >500ms   │ 2 (0.2%)

p50 = 35ms    (median user experience)
p95 = 150ms   (tune for this)
p99 = 280ms   (SLO boundary)
```

### 3. Histogram vs Client-Side Percentiles

| Approach | How it Works | Aggregatable? | Accuracy |
|----------|--------------|---------------|----------|
| **Histogram** | Buckets sent to backend, percentiles calculated server-side | ✅ Yes | ~1-3% error |
| **Client-side** | Percentiles calculated in-app using T-Digest/HDR | ❌ No | More accurate |

**Recommendation:** Use histograms for Prometheus (allows aggregation across instances).

```yaml
# Histogram buckets configuration
management:
  metrics:
    distribution:
      slo:
        http.server.requests: 50ms, 100ms, 200ms, 500ms, 1s
```

### 4. Trace-Based Timing

With distributed tracing, you get breakdown of time spent:

```
POST /api/v1/auth/login (285ms total)
├── [Spring MVC] Controller (285ms)
│   ├── [Auth] Validate input (2ms)
│   ├── [JPA] Find user by username (45ms)
│   │   └── [PostgreSQL] SELECT query (42ms)
│   ├── [BCrypt] Verify password (180ms)  ← CPU-bound
│   ├── [Auth] Generate token (5ms)
│   ├── [JPA] Save session (48ms)
│   │   └── [PostgreSQL] INSERT query (45ms)
│   └── [JSON] Serialize response (5ms)
```

---

## Automation Strategies

### 1. Load Testing Automation

#### Tool Comparison

| Tool | Language | Best For | CI/CD Integration |
|------|----------|----------|-------------------|
| **Gatling** | Scala/Java | High concurrency, detailed reports | Excellent |
| **k6** | JavaScript | Developer-friendly, cloud-native | Excellent |
| **JMeter** | Java/XML | Complex scenarios, GUI available | Good |
| **Locust** | Python | Simple scripts, distributed | Good |

#### Recommended: k6 for Auth Service

**Why k6?**
- JavaScript-based (familiar syntax)
- Low resource usage
- Built-in checks and thresholds
- Native Prometheus/Grafana integration
- Easy CI/CD integration

**Example k6 Test Script:**
```javascript
// auth-load-test.js
import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend } from 'k6/metrics';

// Custom metrics
const loginDuration = new Trend('auth_login_duration');
const loginFailRate = new Rate('auth_login_failures');

export const options = {
    stages: [
        { duration: '1m', target: 50 },   // Ramp up
        { duration: '3m', target: 50 },   // Steady state
        { duration: '1m', target: 100 },  // Spike
        { duration: '1m', target: 0 },    // Ramp down
    ],
    thresholds: {
        http_req_duration: ['p(95)<300', 'p(99)<500'],
        auth_login_failures: ['rate<0.01'],
    },
};

export default function () {
    const loginPayload = JSON.stringify({
        username: `user_${__VU}`,
        password: 'testPassword123',
    });

    const loginRes = http.post(
        'http://localhost:9001/api/v1/auth/login',
        loginPayload,
        { headers: { 'Content-Type': 'application/json' } }
    );

    loginDuration.add(loginRes.timings.duration);
    loginFailRate.add(loginRes.status !== 200);

    check(loginRes, {
        'login successful': (r) => r.status === 200,
        'has token': (r) => JSON.parse(r.body).token !== undefined,
    });

    // Validate token
    if (loginRes.status === 200) {
        const token = JSON.parse(loginRes.body).token;
        const validateRes = http.get(
            'http://localhost:9001/api/v1/auth/validate',
            { headers: { 'Authorization': `Bearer ${token}` } }
        );

        check(validateRes, {
            'token valid': (r) => r.status === 200,
        });
    }

    sleep(1);
}
```

### 2. CI/CD Integration

#### GitHub Actions Workflow
```yaml
# .github/workflows/performance-test.yml
name: Performance Tests

on:
  push:
    branches: [main]
  pull_request:
    branches: [main]

jobs:
  load-test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:14
        env:
          POSTGRES_DB: auth
          POSTGRES_USER: auth
          POSTGRES_PASSWORD: auth
        ports:
          - 5432:5432

    steps:
      - uses: actions/checkout@v4

      - name: Set up Java 25
        uses: actions/setup-java@v4
        with:
          java-version: '25'
          distribution: 'temurin'

      - name: Build Auth Service
        run: cd Auth && ./mvnw package -DskipTests

      - name: Start Auth Service
        run: |
          cd Auth && java -jar target/auth-*.jar &
          sleep 30  # Wait for startup

      - name: Install k6
        run: |
          sudo gpg -k
          sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg \
            --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
          echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" \
            | sudo tee /etc/apt/sources.list.d/k6.list
          sudo apt-get update && sudo apt-get install k6

      - name: Run Load Tests
        run: k6 run --out json=results.json tests/performance/auth-load-test.js

      - name: Check Thresholds
        run: |
          if grep -q '"thresholds":{"http_req_duration":\["p(95)<300"\]:"fail"' results.json; then
            echo "Performance regression detected!"
            exit 1
          fi

      - name: Upload Results
        uses: actions/upload-artifact@v4
        with:
          name: k6-results
          path: results.json
```

### 3. Continuous Performance Monitoring

```
┌─────────────────────────────────────────────────────────────────────┐
│                    PERFORMANCE AUTOMATION PIPELINE                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                      │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │   Code   │───►│  Build   │───►│  Deploy  │───►│  Test    │      │
│  │  Commit  │    │  + Test  │    │  Staging │    │  (k6)    │      │
│  └──────────┘    └──────────┘    └──────────┘    └────┬─────┘      │
│                                                        │             │
│                                                        ▼             │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐    ┌──────────┐      │
│  │  Alert   │◄───│ Compare  │◄───│ Baseline │◄───│ Metrics  │      │
│  │  if Fail │    │ Results  │    │  Check   │    │ Collect  │      │
│  └──────────┘    └──────────┘    └──────────┘    └──────────┘      │
│                                                                      │
└─────────────────────────────────────────────────────────────────────┘
```

### 4. Alerting Rules (Prometheus/Grafana)

```yaml
# prometheus-rules.yml
groups:
  - name: auth-service-alerts
    rules:
      - alert: AuthHighLatency
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/v1/auth/login"}[5m])) > 0.3
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Auth login p99 latency > 300ms"

      - alert: AuthHighErrorRate
        expr: rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m]) > 0.01
        for: 2m
        labels:
          severity: critical
        annotations:
          summary: "Auth service error rate > 1%"

      - alert: AuthTokenValidationSlow
        expr: histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/v1/auth/validate"}[5m])) > 0.05
        for: 5m
        labels:
          severity: warning
        annotations:
          summary: "Token validation p99 > 50ms"
```

---

## Implementation Roadmap

### Phase 1: Foundation (Week 1-2)
- [ ] Add Micrometer Prometheus registry dependency
- [ ] Expose `/actuator/metrics` and `/actuator/prometheus` endpoints
- [ ] Add custom business metrics (login success/failure counters)
- [ ] Set up local Prometheus + Grafana with Docker Compose

### Phase 2: Dashboards & Alerting (Week 2-3)
- [ ] Create Grafana dashboard for Auth service
- [ ] Configure histogram percentiles (p50, p95, p99)
- [ ] Set up alerting rules in Prometheus/Grafana
- [ ] Document SLOs for Auth endpoints

### Phase 3: Load Testing (Week 3-4)
- [ ] Write k6 load test scripts for all Auth endpoints
- [ ] Establish performance baselines
- [ ] Integrate load tests into CI/CD pipeline
- [ ] Configure threshold-based test failures

### Phase 4: Distributed Tracing (Week 4-5)
- [ ] Add OpenTelemetry Java agent or starter
- [ ] Set up Jaeger or Tempo for trace storage
- [ ] Correlate logs with trace IDs
- [ ] Create trace-based dashboards

### Phase 5: Continuous Profiling (Optional, Week 5-6)
- [ ] Evaluate Pyroscope or Grafana Pyroscope
- [ ] Add continuous profiling agent
- [ ] Create flame graph dashboards
- [ ] Identify CPU/memory optimization opportunities

---

## Architecture Diagrams

### Target Observability Stack

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                           OBSERVABILITY STACK                                │
├─────────────────────────────────────────────────────────────────────────────┤
│                                                                              │
│   ┌───────────────────────────────────────────────────────────────────┐    │
│   │                        AUTH SERVICE                                │    │
│   │  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐               │    │
│   │  │ Micrometer  │  │ OTel Agent  │  │  Pyroscope  │               │    │
│   │  │  /prometheus│  │   (traces)  │  │  (profiling)│               │    │
│   │  └──────┬──────┘  └──────┬──────┘  └──────┬──────┘               │    │
│   └─────────┼────────────────┼────────────────┼───────────────────────┘    │
│             │                │                │                             │
│             ▼                ▼                ▼                             │
│   ┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐             │
│   │   Prometheus    │ │     Jaeger/     │ │    Pyroscope    │             │
│   │   (metrics)     │ │     Tempo       │ │    Server       │             │
│   │                 │ │   (traces)      │ │   (profiles)    │             │
│   └────────┬────────┘ └────────┬────────┘ └────────┬────────┘             │
│            │                   │                   │                       │
│            └───────────────────┴───────────────────┘                       │
│                                │                                            │
│                                ▼                                            │
│                    ┌───────────────────────┐                               │
│                    │       GRAFANA         │                               │
│                    │  ┌─────────────────┐  │                               │
│                    │  │   Dashboards    │  │                               │
│                    │  │   Alerting      │  │                               │
│                    │  │   Flame Graphs  │  │                               │
│                    │  └─────────────────┘  │                               │
│                    └───────────────────────┘                               │
│                                                                             │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Docker Compose for Local Development

```yaml
# docker-compose.observability.yml
version: '3.8'

services:
  prometheus:
    image: prom/prometheus:latest
    ports:
      - "9090:9090"
    volumes:
      - ./prometheus.yml:/etc/prometheus/prometheus.yml
    command:
      - '--config.file=/etc/prometheus/prometheus.yml'
      - '--storage.tsdb.path=/prometheus'

  grafana:
    image: grafana/grafana:latest
    ports:
      - "3000:3000"
    environment:
      - GF_SECURITY_ADMIN_PASSWORD=admin
    volumes:
      - grafana-data:/var/lib/grafana
      - ./grafana/dashboards:/etc/grafana/provisioning/dashboards
      - ./grafana/datasources:/etc/grafana/provisioning/datasources

  jaeger:
    image: jaegertracing/all-in-one:latest
    ports:
      - "16686:16686"  # UI
      - "4317:4317"    # OTLP gRPC
      - "4318:4318"    # OTLP HTTP

  pyroscope:
    image: grafana/pyroscope:latest
    ports:
      - "4040:4040"

volumes:
  grafana-data:
```

---

## Appendix: Quick Reference

### Key Metrics to Track

| Metric | Prometheus Query | Alert Threshold |
|--------|------------------|-----------------|
| Login p99 latency | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/v1/auth/login"}[5m]))` | > 300ms |
| Validate p99 latency | `histogram_quantile(0.99, rate(http_server_requests_seconds_bucket{uri="/api/v1/auth/validate"}[5m]))` | > 50ms |
| Error rate | `rate(http_server_requests_seconds_count{status=~"5.."}[5m]) / rate(http_server_requests_seconds_count[5m])` | > 1% |
| Request rate | `rate(http_server_requests_seconds_count[5m])` | N/A (baseline) |
| DB connection pool | `hikaricp_connections_active` | > 80% of max |

### Useful Links

- [Micrometer Documentation](https://docs.micrometer.io/)
- [OpenTelemetry Java](https://opentelemetry.io/docs/languages/java/)
- [Spring Boot Actuator](https://docs.spring.io/spring-boot/reference/actuator/metrics.html)
- [k6 Documentation](https://k6.io/docs/)
- [Grafana Pyroscope](https://grafana.com/docs/pyroscope/latest/)

---

## Decision Required

**Recommended Approach**: Start with **Option A (Micrometer + Prometheus + Grafana)** for immediate value, then layer in **OpenTelemetry for tracing** when needed.

**Questions for Discussion:**
1. Do you want to start with the simpler Prometheus approach or go directly to OpenTelemetry?
2. Which load testing tool preference: k6 (JavaScript) or Gatling (Java/Scala)?
3. Is continuous profiling (Pyroscope) a priority, or should we focus on metrics first?
4. Do you have existing monitoring infrastructure, or do we need to set up everything from scratch?

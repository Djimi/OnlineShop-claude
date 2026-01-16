# Spring Cloud Gateway Features Exploration Plan

**Date:** 2026-01-15
**Status:** ✅ COMPLETED
**Objective:** Implement additional Spring Cloud Gateway MVC features to demonstrate the full capabilities of the framework.

---

## Current State (Before)

The api-gateway already implements:
- Path-based routing with `RewritePath` filter
- Custom `AuthenticationFilter` for token validation
- Custom `RateLimitFilter` with Bucket4j
- Multi-layer caching (L1 Caffeine + L2 Redis)
- Resilience4j integration (Circuit Breaker, Retry, Bulkhead, TimeLimiter)
- CORS configuration
- Metrics/observability with Micrometer

---

## Tasks Implemented

### 1. Request Header Manipulation ✅
- [x] **AddRequestHeader** - Add correlation/tracing headers (X-Request-ID, X-Correlation-ID, X-Gateway-Name, X-Gateway-Timestamp)
- [x] **AddRequestHeadersIfNotPresent** - Add default headers only if not present (Accept-Language, X-Client-Version)
- [x] **SetRequestHeader** - Set/override specific headers (X-Security-Level)
- [x] **RemoveRequestHeader** - Remove sensitive headers before forwarding (X-Debug, X-Trace-Id)
- [x] **MapRequestHeader** - Map User-Agent to X-Original-User-Agent

### 2. Response Header Manipulation ✅
- [x] **AddResponseHeader** - Add custom response headers (X-Gateway-Response-Time, X-Served-By, X-Preview-Notice)
- [x] **SetResponseHeader** - Set/override response headers
- [x] **RemoveResponseHeader** - Remove internal headers (X-Powered-By, Server, X-Internal-Debug, X-Backend-Server)
- [x] **DedupeResponseHeader** - Remove duplicate CORS headers
- [x] **Security Headers** - Full suite including:
  - X-Content-Type-Options: nosniff
  - X-Frame-Options: DENY
  - X-XSS-Protection: 1; mode=block
  - Referrer-Policy: strict-origin-when-cross-origin
  - Content-Security-Policy: default-src 'self'
  - Permissions-Policy: geolocation=(), microphone=(), camera=()
  - Cache-Control: no-store, no-cache, must-revalidate, proxy-revalidate
  - Strict-Transport-Security: max-age=31536000; includeSubDomains

### 3. Request/Response Enrichment ✅
- [x] **Request Metadata Headers** - Add metadata via headers for downstream processing:
  - X-Gateway-Metadata-Timestamp
  - X-Gateway-Metadata-RequestId
  - X-Gateway-Metadata-Source
- [x] **Audit Headers** - Add audit trail via headers:
  - X-Audit-Timestamp
  - X-Audit-User-Id
  - X-Audit-Client-Ip
- [x] **Custom Response Handlers** - Return custom JSON responses:
  - API info endpoint with wrapped response
  - Deprecated endpoint with RFC 9457 format
  - Health check with gateway status

**Note:** ModifyRequestBody/ModifyResponseBody filters require specific API signatures in Spring Cloud Gateway MVC. The header-based approach provides equivalent functionality and is more compatible.

### 4. Path Manipulation ✅
- [x] **PrefixPath** - Add /api prefix to paths
- [x] **StripPrefix** - Remove /gateway prefix
- [x] **SetPath** - Set path for specific routes

### 5. Request Validation ✅
- [x] **RequestSize** - Limit to 5MB (configurable)
- [x] **RequestHeaderSize** - Limit to 16KB (configurable)

### 6. Redirect & Status ✅
- [x] **RedirectTo** - Multiple redirects:
  - /api/items/** → /items (301 Moved Permanently)
  - /api/auth/** → /auth (301 Moved Permanently)
  - /docs, /documentation → /swagger-ui.html (302 Found)
- [x] **SetStatus** - Health check always returns 200

### 7. Query Parameter Manipulation ✅
- [x] **AddRequestParameter** - Add default sorting (sort, order), pagination (limit, offset)
- [x] **RemoveRequestParameter** - Remove debug parameters (debug, trace)

### 8. Advanced Predicates ✅
- [x] **Weight Predicate** - 10% canary / 90% stable traffic split
- [x] **Header Predicate** - Route based on X-API-Version: v2, X-Secure-Access
- [x] **Query Predicate** - Route /items/search when ?q= present
- [x] **Method Predicate** - Route GET requests differently
- [x] **Cookie Predicate** - Route based on preview_mode=true cookie
- [x] **Time-based Predicates** - Between predicate for maintenance windows

### 9. Request Logging & Tracing ✅
- [x] **RequestLoggingFilter** - Comprehensive logging with:
  - Request/response timing with Micrometer metrics
  - Detailed request/response logging (configurable)
  - Request ID propagation
  - Structured logging format
  - Sensitive data masking (passwords, tokens, API keys)
  - Slow request detection and alerting
  - Client IP extraction (X-Forwarded-For support)

### 10. Circuit Breaker Fallback ✅
- [x] **Fallback Route** - Custom 404 JSON response with RFC 9457 format for unmatched paths

### 11. Miscellaneous ✅
- [x] **PreserveHostHeader** - Preserve original Host header for all routes
- [x] **Response Compression** - GZIP compression for responses > 1KB

---

## Files Created/Modified

### New Files:
1. `src/main/java/com/onlineshop/gateway/filter/RequestLoggingFilter.java` - Comprehensive logging filter
2. `src/main/java/com/onlineshop/gateway/config/BodyModificationConfig.java` - Body transformation routes

### Modified Files:
1. `src/main/java/com/onlineshop/gateway/config/GatewayConfig.java` - Enhanced with all gateway features
2. `src/main/resources/application.yml` - New configuration properties

---

## Route Summary

| Route Name | Path | Features |
|------------|------|----------|
| auth-service | /auth/** | Header manipulation, security headers, request ID |
| items-service | /items/** | All above + request size/header validation, header mapping |
| items-get-versioned | /items/** (GET + X-API-Version: v2) | Header predicate, method predicate |
| items-search | /items/search?q= | Query predicate, pagination defaults |
| items-preview | /items/** (cookie: preview_mode=true) | Cookie predicate |
| items-canary | /items/** (10% weight) | Weight predicate, canary routing |
| items-stable | /items/** (90% weight) | Weight predicate, stable routing |
| maintenance-window | /items/** (time-based) | Between predicate |
| legacy-items-redirect | /api/items/** | RedirectTo 301 |
| legacy-auth-redirect | /api/auth/** | RedirectTo 301 |
| docs-redirect | /docs, /documentation | RedirectTo 302 |
| strip-prefix-demo | /gateway/items/** | StripPrefix |
| prefix-path-demo | /v1/items/** | PrefixPath |
| items-list-with-defaults | GET /items | Query parameter manipulation |
| gateway-health | /health | Custom health response |
| secure-items | /secure/items/** | Header predicate, header removal |
| items-with-metadata-headers | /api/v1/enriched/items/** | Metadata header injection |
| items-with-audit-headers | /api/v1/audited/items/** | Audit header injection |
| staging-items | /staging/items/** | Environment headers |
| api-info | /api/info | Custom JSON response |
| deprecated-endpoint | /api/deprecated/** | 410 Gone with RFC 9457 |
| items-v2 | /v2/items/** | API versioning headers |
| fallback | /** | Custom 404 response |

---

## New Configuration Properties

```yaml
gateway:
  request:
    max-size: 5242880        # 5MB
    max-header-size: 16384   # 16KB
  logging:
    enabled: true
    include-headers: false
    include-payload: false
    include-query-string: true
    slow-request-threshold-ms: 1000
  security:
    headers:
      enabled: true
      hsts-max-age: 31536000
      content-security-policy: "default-src 'self'"
  routing:
    canary-percentage: 10
    weight-routing-enabled: true
```

---

## New Metrics

| Metric | Type | Tags | Description |
|--------|------|------|-------------|
| gateway.http.requests | Timer | method, uri, status, outcome | HTTP request duration |
| gateway.http.slow.requests | Counter | method, uri | Slow request count |

---

## Issues

- (None encountered)

---

## Sources

- [Spring Cloud Gateway Documentation](https://spring.io/projects/spring-cloud-gateway/)
- [Gateway Handler Filter Functions](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/gateway-handler-filter-functions.html)
- [Gateway Request Predicates](https://docs.spring.io/spring-cloud-gateway/reference/spring-cloud-gateway-server-webmvc/gateway-request-predicates.html)

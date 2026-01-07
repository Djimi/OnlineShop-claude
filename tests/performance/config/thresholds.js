/**
 * SLO (Service Level Objectives) Thresholds for Auth Service
 *
 * These thresholds define the performance requirements for each endpoint.
 * Tests will fail if these thresholds are not met.
 *
 * Uses k6's built-in http_req_duration with operation tags for per-endpoint metrics.
 */

export const AUTH_THRESHOLDS = {
    // Login endpoint - includes Argon2id password verification
    login: {
        p95: 400,  // 95% of requests under 400ms
        p99: 600,  // 99% of requests under 600ms
        errorRate: 0.01,  // < 1% errors
    },

    // Validate endpoint - critical path, must be fast
    validate: {
        p95: 70,   // 95% of requests under 70ms
        p99: 150,  // 99% of requests under 150ms
        errorRate: 0.001,  // < 0.1% errors (most critical)
    },

    // Register endpoint - includes Argon2id hashing (CPU intensive)
    register: {
        p95: 400,  // 95% of requests under 400ms
        p99: 700,  // 99% of requests under 700ms
        errorRate: 0.02,  // < 2% errors (duplicates expected)
    },
};

/**
 * Generate k6 threshold configuration object for load tests
 */
export function generateThresholds() {
    return {
        // Per-operation duration thresholds (using tags)
        'http_req_duration{operation:login}': [
            `p(95)<${AUTH_THRESHOLDS.login.p95}`,
            `p(99)<${AUTH_THRESHOLDS.login.p99}`,
        ],
        'http_req_duration{operation:validate}': [
            `p(95)<${AUTH_THRESHOLDS.validate.p95}`,
            `p(99)<${AUTH_THRESHOLDS.validate.p99}`,
        ],
        'http_req_duration{operation:register}': [
            `p(95)<${AUTH_THRESHOLDS.register.p95}`,
            `p(99)<${AUTH_THRESHOLDS.register.p99}`,
        ],

        // Per-operation error rate thresholds
        'http_req_failed{operation:login}': [`rate<${AUTH_THRESHOLDS.login.errorRate}`],
        'http_req_failed{operation:validate}': [`rate<${AUTH_THRESHOLDS.validate.errorRate}`],
        'http_req_failed{operation:register}': [`rate<${AUTH_THRESHOLDS.register.errorRate}`],
    };
}

/**
 * Smoke test thresholds - more lenient for quick sanity checks
 */
export function generateSmokeThresholds() {
    return {
        // Per-operation duration (lenient for smoke)
        'http_req_duration{operation:login}': ['p(95)<400'],
        'http_req_duration{operation:validate}': ['p(95)<70'],
        'http_req_duration{operation:register}': ['p(95)<400'],

        // Per-operation error rates (lenient)
        'http_req_failed{operation:login}': ['rate<0.1'],
        'http_req_failed{operation:validate}': ['rate<0.01'],
        'http_req_failed{operation:register}': ['rate<0.01'],
    };
}

/**
 * Stress test thresholds - we expect degradation under extreme load
 */
export function generateStressThresholds() {
    return {
        // Per-operation duration (very lenient under stress)
        'http_req_duration{operation:login}': ['p(95)<2000'],
        'http_req_duration{operation:validate}': ['p(95)<500'],
        'http_req_duration{operation:register}': ['p(95)<2000'],

        // Per-operation error rates (allow degradation)
        'http_req_failed{operation:login}': ['rate<0.30'],
        'http_req_failed{operation:validate}': ['rate<0.20'],
        'http_req_failed{operation:register}': ['rate<0.40'],
    };
}

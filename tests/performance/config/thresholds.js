/**
 * SLO (Service Level Objectives) Thresholds for Auth Service
 *
 * These thresholds define the performance requirements for each endpoint.
 * Tests will fail if these thresholds are not met.
 */

export const AUTH_THRESHOLDS = {
    // Login endpoint - includes BCrypt password verification
    login: {
        p95: 300,  // 95% of requests under 300ms
        p99: 500,  // 99% of requests under 500ms
    },

    // Validate endpoint - critical path, must be fast
    validate: {
        p95: 50,   // 95% of requests under 50ms
        p99: 100,  // 99% of requests under 100ms
    },

    // Register endpoint - includes BCrypt hashing (CPU intensive)
    register: {
        p95: 500,  // 95% of requests under 500ms
        p99: 800,  // 99% of requests under 800ms
    },
};

/**
 * Generate k6 threshold configuration object
 */
export function generateThresholds(includeCustomMetrics = true) {
    const thresholds = {
        // Global HTTP thresholds
        'http_req_duration': ['p(95)<500', 'p(99)<1000'],
        'http_req_failed': ['rate<0.01'],  // Less than 1% errors
    };

    if (includeCustomMetrics) {
        // Custom metric thresholds - Duration
        thresholds['auth_login_duration'] = [
            `p(95)<${AUTH_THRESHOLDS.login.p95}`,
            `p(99)<${AUTH_THRESHOLDS.login.p99}`,
        ];
        thresholds['auth_validate_duration'] = [
            `p(95)<${AUTH_THRESHOLDS.validate.p95}`,
            `p(99)<${AUTH_THRESHOLDS.validate.p99}`,
        ];
        thresholds['auth_register_duration'] = [
            `p(95)<${AUTH_THRESHOLDS.register.p95}`,
            `p(99)<${AUTH_THRESHOLDS.register.p99}`,
        ];

        // Operation-specific error rate thresholds using tags
        // Validate is most critical - should have near-zero errors
        thresholds['errors{operation:validate}'] = ['rate<0.001'];  // < 0.1% errors

        // Login should have very low errors
        thresholds['errors{operation:login}'] = ['rate<0.01'];  // < 1% errors

        // Register can tolerate slightly more (duplicate usernames, validation)
        thresholds['errors{operation:register}'] = ['rate<0.02'];  // < 2% errors
    }

    return thresholds;
}

/**
 * Smoke test thresholds - more lenient for quick sanity checks
 */
export function generateSmokeThresholds() {
    return {
        'http_req_duration': ['p(95)<2000'],  // Very generous for smoke
        'http_req_failed': ['rate<0.10'],      // Allow up to 10% errors in smoke

        // Tagged error rates for smoke tests (lenient)
        'errors{operation:validate}': ['rate<0.05'],  // < 5% errors
        'errors{operation:login}': ['rate<0.10'],      // < 10% errors
        'errors{operation:register}': ['rate<0.10'],   // < 10% errors
    };
}

/**
 * Stress test thresholds - we expect degradation under extreme load
 */
export function generateStressThresholds() {
    return {
        'http_req_duration': ['p(95)<3000'],  // Allow up to 3s under stress
        'http_req_failed': ['rate<0.30'],     // Allow up to 30% errors

        // Tagged error rates for stress tests (very lenient)
        'errors{operation:validate}': ['rate<0.20'],  // < 20% errors
        'errors{operation:login}': ['rate<0.30'],      // < 30% errors
        'errors{operation:register}': ['rate<0.40'],   // < 40% errors (duplicate conflicts expected)
    };
}

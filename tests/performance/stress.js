/**
 * Stress Test for Auth Service
 *
 * Purpose: Push the service beyond expected load to find breaking points
 * and understand system behavior under extreme conditions.
 *
 * Characteristics:
 * - Gradual ramp up beyond normal capacity
 * - Sustain at breaking point
 * - Push beyond breaking point
 * - Observe recovery
 * - Duration: ~20 minutes
 * - Lenient thresholds (we expect some failures)
 *
 * Traffic Mix:
 * - 80% Token validation (validate)
 * - 10% Login attempts (login)
 * - 10% New registrations (register)
 *
 * Usage:
 *   k6 run stress.js
 *   k6 run -e ENV=docker stress.js
 */

import { group, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { getAuthServiceUrl } from './config/environments.js';
import { generateStressThresholds } from './config/thresholds.js';
import {
    login,
    validateToken,
    register,
    extractToken,
    checkLoginResponse,
    checkValidateResponse,
    checkRegisterResponse,
    generateUniqueUsername,
    randomIntBetween,
} from './utils/helpers.js';
import {
    recordLoginMetrics,
    recordValidateMetrics,
    recordRegisterMetrics,
} from './utils/metrics.js';

const BASE_URL = getAuthServiceUrl();
const EARLY_ABORT = __ENV.EARLY_ABORT === 'true';

// Traffic distribution
const TRAFFIC_MIX = {
    validate: 0.80,  // 80%
    login: 0.10,     // 10%
    register: 0.10,  // 10%
};

export const options = {
    setupTimeout: '10m',
    scenarios: {
        stress_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                // Phase 1: Ramp up to normal load
                { duration: '2m', target: 50 },

                // Phase 2: Stay at normal load (baseline)
                { duration: '3m', target: 50 },

                // Phase 3: Ramp up to high load
                { duration: '2m', target: 100 },

                // Phase 4: Stay at high load
                { duration: '3m', target: 100 },

                // Phase 5: Ramp up to stress level
                { duration: '2m', target: 200 },

                // Phase 6: Stay at stress level
                { duration: '3m', target: 200 },

                // Phase 7: Spike to extreme
                { duration: '1m', target: 300 },

                // Phase 8: Stay at extreme briefly
                { duration: '2m', target: 300 },

                // Phase 9: Recovery - ramp down
                { duration: '2m', target: 0 },
            ],
            gracefulRampDown: '1m',
        },
    },
    thresholds: EARLY_ABORT ? {
        ...generateStressThresholds(),
        // Stop early when stress behavior is clearly beyond acceptable bounds.
        'http_req_duration{operation:validate}': [
            { threshold: 'p(95)<2500', abortOnFail: true, delayAbortEval: '4m' },
        ],
        'http_req_failed{operation:validate}': [
            { threshold: 'rate<0.30', abortOnFail: true, delayAbortEval: '4m' },
        ],
        'http_req_duration{operation:login}': [
            { threshold: 'p(95)<4000', abortOnFail: true, delayAbortEval: '4m' },
        ],
        'http_req_duration{operation:register}': [
            { threshold: 'p(95)<4000', abortOnFail: true, delayAbortEval: '4m' },
        ],
    } : generateStressThresholds(),
};

// VU-local token cache
let cachedToken = null;
let cachedTokenExpiry = 0;

export function setup() {
    console.log('='.repeat(60));
    console.log('STRESS TEST - Auth Service');
    console.log('='.repeat(60));
    console.log(`Target: ${BASE_URL}`);
    console.log('');
    console.log('Test Phases:');
    console.log('  Phase 1-2: Normal load (50 VUs)');
    console.log('  Phase 3-4: High load (100 VUs)');
    console.log('  Phase 5-6: Stress level (200 VUs)');
    console.log('  Phase 7-8: Extreme load (300 VUs)');
    console.log('  Phase 9:   Recovery');
    console.log(`  Early abort: ${EARLY_ABORT ? 'enabled' : 'disabled'}`);
    console.log('');

    // Create test users
    const testUsers = [];
    for (let i = 1; i <= 10; i++) {
        const user = {
            username: `stresstest_user_${i}`,
            password: 'StressTest123!',
        };

        const registerRes = register(BASE_URL, user.username, user.password);
        if (registerRes.status === 201) {
            console.log(`Created test user: ${user.username}`);
        } else if (registerRes.status === 409) {
            console.log(`Test user exists: ${user.username}`);
        }

        testUsers.push(user);
    }

    return { testUsers };
}

export default function (data) {
    const { testUsers } = data;
    const testUser = testUsers[Math.floor(Math.random() * testUsers.length)];

    const action = Math.random();

    if (action < TRAFFIC_MIX.register) {
        performRegister();
    } else if (action < TRAFFIC_MIX.register + TRAFFIC_MIX.login) {
        performLogin(testUser);
    } else {
        performValidate(testUser);
    }

    // Shorter think time under stress to increase pressure
    sleep(randomIntBetween(0.5, 1.5));
}

function performLogin(testUser) {
    group('Login', function () {
        const res = login(BASE_URL, testUser.username, testUser.password);
        const success = checkLoginResponse(res, 'stress_login');

        recordLoginMetrics(res, success);

        if (success) {
            cachedToken = extractToken(res);
            cachedTokenExpiry = Date.now() + 3500000;
        }
    });
}

function performValidate(testUser) {
    group('Validate', function () {
        if (!cachedToken || Date.now() > cachedTokenExpiry) {
            const loginRes = login(BASE_URL, testUser.username, testUser.password);
            if (loginRes.status === 200) {
                cachedToken = extractToken(loginRes);
                cachedTokenExpiry = Date.now() + 3500000;
            } else {
                recordValidateMetrics({ timings: { duration: 0 } }, false);
                return;
            }
        }

        const res = validateToken(BASE_URL, cachedToken);
        const success = checkValidateResponse(res, 'stress_validate');

        recordValidateMetrics(res, success);

        if (!success && res.status === 401) {
            cachedToken = null;
            cachedTokenExpiry = 0;
        }
    });
}

function performRegister() {
    group('Register', function () {
        const newUsername = generateUniqueUsername('stress');
        const res = register(BASE_URL, newUsername, 'StressTest123!');
        const success = checkRegisterResponse(res, 'stress_register');

        recordRegisterMetrics(res, success);
    });
}

export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('STRESS TEST COMPLETE');
    console.log('='.repeat(60));
    console.log('');
    console.log('Review the metrics to identify:');
    console.log('  - At what load level did response times degrade?');
    console.log('  - At what load level did errors start occurring?');
    console.log('  - How quickly did the system recover?');
    console.log('');
}

export function handleSummary(data) {
    const now = new Date().toISOString().replace(/[:.]/g, '-');

    return {
        [`reports/stress-${now}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

/**
 * Load Test for Auth Service
 *
 * Purpose: Test the service under expected load with realistic traffic patterns.
 * Run this nightly or before releases.
 *
 * Traffic Mix (as specified):
 * - 80% Token validation (validate)
 * - 10% Login attempts (login)
 * - 10% New registrations (register)
 *
 * Characteristics:
 * - Ramp up to target VUs
 * - Sustained load period
 * - Ramp down
 * - Duration: ~10 minutes
 * - Strict SLO thresholds
 *
 * Usage:
 *   k6 run load.js
 *   k6 run -e ENV=docker load.js
 *   k6 run --out json=results.json load.js
 */

import { group, sleep } from 'k6';
import { textSummary } from 'https://jslib.k6.io/k6-summary/0.0.1/index.js';
import { getAuthServiceUrl } from './config/environments.js';
import { generateThresholds } from './config/thresholds.js';
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

// Traffic distribution (must sum to 1.0)
const TRAFFIC_MIX = {
    validate: 0.80,  // 80%
    login: 0.10,     // 10%
    register: 0.10,  // 10%
};

export const options = {
    scenarios: {
        load_test: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '1m', target: 20 },   // Ramp up to 20 VUs
                { duration: '3m', target: 50 },   // Ramp up to 50 VUs
                { duration: '5m', target: 50 },   // Stay at 50 VUs (steady state)
                { duration: '1m', target: 0 },    // Ramp down to 0
            ],
            gracefulRampDown: '30s',
        },
    },
    thresholds: generateThresholds(),
};

// Shared state for tokens (VU-local)
let cachedToken = null;
let cachedTokenExpiry = 0;

// Setup: Create test users
export function setup() {
    console.log('='.repeat(60));
    console.log('LOAD TEST - Auth Service');
    console.log('='.repeat(60));
    console.log(`Target: ${BASE_URL}`);
    console.log(`Traffic Mix: ${TRAFFIC_MIX.validate * 100}% validate, ${TRAFFIC_MIX.login * 100}% login, ${TRAFFIC_MIX.register * 100}% register`);
    console.log('');

    // Create multiple test users for load distribution
    const testUsers = [];
    for (let i = 1; i <= 5; i++) {
        const user = {
            username: `loadtest_user_${i}`,
            password: 'LoadTest123!',
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

    // Select a random test user for this iteration
    const testUser = testUsers[Math.floor(Math.random() * testUsers.length)];

    // Determine which action to perform based on traffic mix
    const action = Math.random();

    if (action < TRAFFIC_MIX.register) {
        // 10% - Registration
        performRegister();
    } else if (action < TRAFFIC_MIX.register + TRAFFIC_MIX.login) {
        // 10% - Login
        performLogin(testUser);
    } else {
        // 80% - Validate
        performValidate(testUser);
    }

    // Think time - simulate realistic user behavior
    sleep(randomIntBetween(1, 3));
}

/**
 * Perform login operation
 */
function performLogin(testUser) {
    group('Login', function () {
        const res = login(BASE_URL, testUser.username, testUser.password);
        const success = checkLoginResponse(res, 'load_login');

        recordLoginMetrics(res, success);

        // Cache the token for validate operations
        if (success) {
            cachedToken = extractToken(res);
            cachedTokenExpiry = Date.now() + 3500000; // ~58 minutes (slightly less than 1h expiry)
        }
    });
}

/**
 * Perform token validation
 */
function performValidate(testUser) {
    group('Validate', function () {
        // Check if we have a valid cached token
        if (!cachedToken || Date.now() > cachedTokenExpiry) {
            // Need to login first to get a token
            const loginRes = login(BASE_URL, testUser.username, testUser.password);
            if (loginRes.status === 200) {
                cachedToken = extractToken(loginRes);
                cachedTokenExpiry = Date.now() + 3500000;
            } else {
                // Can't proceed without a token
                recordValidateMetrics({ timings: { duration: 0 } }, false);
                return;
            }
        }

        const res = validateToken(BASE_URL, cachedToken);
        const success = checkValidateResponse(res, 'load_validate');

        recordValidateMetrics(res, success);

        // If validation failed due to invalid/expired token, clear cache
        if (!success && res.status === 401) {
            cachedToken = null;
            cachedTokenExpiry = 0;
        }
    });
}

/**
 * Perform registration
 */
function performRegister() {
    group('Register', function () {
        const newUsername = generateUniqueUsername('load');
        const res = register(BASE_URL, newUsername, 'LoadTest123!');
        const success = checkRegisterResponse(res, 'load_register');

        recordRegisterMetrics(res, success);
    });
}

export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('LOAD TEST COMPLETE');
    console.log('='.repeat(60));
}

export function handleSummary(data) {
    const now = new Date().toISOString().replace(/[:.]/g, '-');

    return {
        [`reports/load-${now}.json`]: JSON.stringify(data, null, 2),
        stdout: textSummary(data, { indent: ' ', enableColors: true }),
    };
}

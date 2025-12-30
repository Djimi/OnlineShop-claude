/**
 * Smoke Test for Auth Service
 *
 * Purpose: Quick sanity check to verify the service is up and basic
 * functionality works. Run this on every PR.
 *
 * Characteristics:
 * - 1-2 VUs (minimal load)
 * - ~10 iterations
 * - Duration: ~1-2 minutes
 * - Lenient thresholds
 *
 * Usage:
 *   k6 run smoke.js
 *   k6 run -e ENV=docker smoke.js
 */

import { group, sleep } from 'k6';
import { getAuthServiceUrl } from './config/environments.js';
import { generateSmokeThresholds } from './config/thresholds.js';
import {
    login,
    validateToken,
    register,
    extractToken,
    checkLoginResponse,
    checkValidateResponse,
    checkRegisterResponse,
    generateUniqueUsername,
} from './utils/helpers.js';
import {
    errorRate,
    loginDuration,
    validateDuration,
    registerDuration,
} from './utils/metrics.js';

const BASE_URL = getAuthServiceUrl();

export const options = {
    vus: 1,
    iterations: 10,
    thresholds: generateSmokeThresholds(),
};

// Setup: Ensure we have a test user (runs once before VU code)
export function setup() {
    console.log('='.repeat(60));
    console.log('SMOKE TEST - Auth Service');
    console.log('='.repeat(60));
    console.log(`Target: ${BASE_URL}`);
    console.log('');

    // Create a test user for login tests
    const testUser = {
        username: 'smoke_test_user',
        password: 'SmokeTest123!',
    };

    // Try to register the test user (may already exist)
    const registerRes = register(BASE_URL, testUser.username, testUser.password);
    if (registerRes.status === 201) {
        console.log(`Created test user: ${testUser.username}`);
    } else if (registerRes.status === 409) {
        console.log(`Test user already exists: ${testUser.username}`);
    } else {
        console.warn(`Failed to create test user: ${registerRes.status}`);
    }

    return { testUser };
}

export default function (data) {
    const { testUser } = data;

    group('1. Health Check', function () {
        // Just verify the service responds
        const healthRes = login(BASE_URL, 'nonexistent', 'wrongpass');
        // We expect 401 for wrong credentials, which means service is up
        if (healthRes.status === 401 || healthRes.status === 200) {
            console.log('Health check: Service is responding');
        } else {
            console.error(`Health check failed: ${healthRes.status}`);
            errorRate.add(true);
        }
    });

    group('2. Login Flow', function () {
        const loginRes = login(BASE_URL, testUser.username, testUser.password);
        const loginSuccess = checkLoginResponse(loginRes, 'smoke_login');

        loginDuration.add(loginRes.timings.duration);
        errorRate.add(!loginSuccess);

        if (loginSuccess) {
            console.log(`Login successful (${loginRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`Login failed: ${loginRes.status} - ${loginRes.body}`);
        }
    });

    group('3. Validate Flow', function () {
        // First login to get a token
        const loginRes = login(BASE_URL, testUser.username, testUser.password);
        const token = extractToken(loginRes);

        if (token) {
            const validateRes = validateToken(BASE_URL, token);
            const validateSuccess = checkValidateResponse(validateRes, 'smoke_validate');

            validateDuration.add(validateRes.timings.duration);
            errorRate.add(!validateSuccess);

            if (validateSuccess) {
                console.log(`Validate successful (${validateRes.timings.duration.toFixed(0)}ms)`);
            } else {
                console.error(`Validate failed: ${validateRes.status}`);
            }
        } else {
            console.error('Could not get token for validate test');
            errorRate.add(true);
        }
    });

    group('4. Register Flow', function () {
        const newUsername = generateUniqueUsername('smoke');
        const registerRes = register(BASE_URL, newUsername, 'SmokeTest123!');
        const registerSuccess = checkRegisterResponse(registerRes, 'smoke_register');

        registerDuration.add(registerRes.timings.duration);
        errorRate.add(!registerSuccess);

        if (registerSuccess) {
            console.log(`Register successful (${registerRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`Register failed: ${registerRes.status}`);
        }
    });

    sleep(1);
}

export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('SMOKE TEST COMPLETE');
    console.log('='.repeat(60));
}

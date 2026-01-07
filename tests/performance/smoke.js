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
import exec from 'k6/execution';
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
    recordLoginMetrics,
    recordValidateMetrics,
    recordRegisterMetrics,
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

    // Create a test user for login tests with unique username
    // Format: testUser_<ISO8601-date>_<vu-id>
    const timestamp = new Date().toISOString().split('.')[0]; // ISO 8601: YYYY-MM-DDTHH:MM:SS
    const vuId = exec.vu.idInTest;

    const testUser = {
        username: `testUser_${timestamp}_${vuId}`,
        password: 'SmokeTest123!',
    };

    // Try to register the test user (should not already exist due to unique username)
    const registerRes = register(BASE_URL, testUser.username, testUser.password);
    if (registerRes.status === 201) {
        console.log(`Created test user: ${testUser.username}`);
    } else {
        throw new Error(`Failed to create test user: ${registerRes.status} - ${registerRes.body}`);
    }

    return { testUser };
}

export default function (data) {
    const { testUser } = data;
    const vuid = exec.vu.idInTest;
    const iter = exec.vu.iterationInInstance;

    group('1. Login Flow', function () {
        const loginRes = login(BASE_URL, testUser.username, testUser.password);
        const loginSuccess = checkLoginResponse(loginRes, 'smoke_login');

        // Record all login metrics in one call
        recordLoginMetrics(loginRes, loginSuccess, { vuid: vuid });

        if (loginSuccess) {
            console.log(`[VU ${vuid}, Iter ${iter}] Login successful (${loginRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`[VU ${vuid}, Iter ${iter}] Login failed: ${loginRes.status} - ${loginRes.body}`);
        }
    });

    group('2. Validate Flow', function () {
        // First login to get a token
        const loginRes = login(BASE_URL, testUser.username, testUser.password);
        const token = extractToken(loginRes);

        if (token) {
            const validateRes = validateToken(BASE_URL, token);
            const validateSuccess = checkValidateResponse(validateRes, 'smoke_validate');

            // Record all validate metrics in one call
            recordValidateMetrics(validateRes, validateSuccess, { vuid: vuid });

            if (validateSuccess) {
                console.log(`[VU ${vuid}, Iter ${iter}] Validate successful (${validateRes.timings.duration.toFixed(0)}ms)`);
            } else {
                console.error(`[VU ${vuid}, Iter ${iter}] Validate failed: ${validateRes.status}`);
            }
        } else {
            console.error(`[VU ${vuid}, Iter ${iter}] Could not get token for validate test`);
            // Record as a failed validation since we couldn't get a token
            recordValidateMetrics(
                { timings: { duration: 0 }, status: 0 },
                false,
                { vuid: vuid, error: 'no_token' }
            );
        }
    });

    group('3. Register Flow', function () {
        const newUsername = generateUniqueUsername('smoke');
        const registerRes = register(BASE_URL, newUsername, 'SmokeTest123!');
        const registerSuccess = checkRegisterResponse(registerRes, 'smoke_register');

        // Record all register metrics in one call
        recordRegisterMetrics(registerRes, registerSuccess, { vuid: vuid });

        if (registerSuccess) {
            console.log(`[VU ${vuid}, Iter ${iter}] Register successful (${registerRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`[VU ${vuid}, Iter ${iter}] Register failed: ${registerRes.status}`);
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

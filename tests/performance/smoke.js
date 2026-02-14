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
 *   k6 run smoke-1vu.js
 *   k6 run -e ENV=docker smoke-1vu.js
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
    checkValidateResponseWithExpectedValidity,
    checkRegisterResponse,
    generateUniqueUsername,
} from './utils/helpers.js';
import {
    recordLoginMetrics,
    recordValidateMetrics,
    recordRegisterMetrics,
} from './utils/metrics.js';

const BASE_URL = getAuthServiceUrl();
const SEEDED_USER_COUNT = 7;

// VU-local state initialized from setup data on first iteration
let seededUsers = [];
let tokenPool = [];
let seedInitialized = false;

export const options = {
    vus: 1,
    iterations: 10,
    thresholds: generateSmokeThresholds(),
};

// Setup: Prepare seeded users and tokens once before VU code.
export function setup() {
    console.log('='.repeat(60));
    console.log('SMOKE TEST - Auth Service');
    console.log('='.repeat(60));
    console.log(`Target: ${BASE_URL}`);
    console.log('');

    const seedRunId = Date.now();
    const users = [];
    const validTokens = [];

    for (let i = 0; i < SEEDED_USER_COUNT; i++) {
        const user = {
            username: `smoke_seed_${seedRunId}_${i + 1}`,
            password: 'SmokeTest123!',
        };

        const registerRes = register(BASE_URL, user.username, user.password);
        if (registerRes.status !== 201 && registerRes.status !== 409) {
            throw new Error(`Failed to create seed user ${user.username}: ${registerRes.status} - ${registerRes.body}`);
        }

        const loginRes = login(BASE_URL, user.username, user.password);
        const loginSuccess = checkLoginResponse(loginRes, 'setup_seed_login', { operation: 'login_setup' });
        const token = loginSuccess ? extractToken(loginRes) : null;
        if (!token) {
            throw new Error(`Failed to create seed token for user ${user.username}: ${loginRes.status} - ${loginRes.body}`);
        }

        users.push(user);
        validTokens.push(token);
    }

    console.log(`Prepared ${users.length} seed users with valid tokens for smoke validate flow`);
    return { users, validTokens };
}

export default function (data) {
    const vuid = exec.vu.idInTest;
    const iter = exec.vu.iterationInInstance;

    if (!seedInitialized) {
        seededUsers = data.users;
        tokenPool = data.validTokens.slice();
        seedInitialized = true;
    }

    const loginUserIndex = iter % seededUsers.length;
    const loginUser = seededUsers[loginUserIndex];

    // 1 Login
    group('Login Flow', function () {
        const loginRes = login(BASE_URL, loginUser.username, loginUser.password);
        const loginSuccess = checkLoginResponse(loginRes);
        const refreshedToken = loginSuccess ? extractToken(loginRes) : null;

        recordLoginMetrics(loginRes, loginSuccess, { vuid: vuid });

        // Keep one token per seeded user so validate calls use distinct valid tokens.
        if (refreshedToken) {
            tokenPool[loginUserIndex] = refreshedToken;
        }

        if (loginSuccess) {
            console.log(`[VU ${vuid}, Iter ${iter}] Login successful (${loginRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`[VU ${vuid}, Iter ${iter}] Login failed: ${loginRes.status} - ${loginRes.body}`);
        }
    });

    // 1 Register
    group('Register Flow', function () {
        const newUsername = generateUniqueUsername('smoke');
        const registerRes = register(BASE_URL, newUsername, 'SmokeTest123!');
        const registerSuccess = checkRegisterResponse(registerRes);

        recordRegisterMetrics(registerRes, registerSuccess, { vuid: vuid });

        if (registerSuccess) {
            console.log(`[VU ${vuid}, Iter ${iter}] Register successful (${registerRes.timings.duration.toFixed(0)}ms)`);
        } else {
            console.error(`[VU ${vuid}, Iter ${iter}] Register failed: ${registerRes.status}`);
        }
    });

    const invalidToken = `invalid_smoke_token_${Date.now()}_${iter}_${Math.random().toString(36).substring(2, 10)}`;
    const validationPlan = tokenPool.map((token, index) => ({
        token: token,
        expectedValid: true,
        label: `Validate Valid Token ${index + 1}`,
    }));
    validationPlan.push({
        token: invalidToken,
        expectedValid: false,
        label: 'Validate Invalid Token',
    });

    // 8 Validates total: 7 valid tokens + 1 invalid token
    for (let i = 0; i < validationPlan.length; i++) {
        const plan = validationPlan[i];
        group(`Validate Flow ${i + 1}`, function () {
            const validateRes = validateToken(BASE_URL, plan.token);
            const validateSuccess = checkValidateResponseWithExpectedValidity(
                validateRes,
                plan.expectedValid,
                plan.expectedValid ? 'smoke_validate_valid' : 'smoke_validate_invalid_expected_false',
                plan.expectedValid ? { operation: 'validate' } : { operation: 'validate_invalid' }
            );

            recordValidateMetrics(
                validateRes,
                validateSuccess,
                { vuid: vuid, expected_valid: String(plan.expectedValid) }
            );

            if (validateSuccess) {
                console.log(`[VU ${vuid}, Iter ${iter}] ${plan.label} successful (${validateRes.timings.duration.toFixed(0)}ms)`);
            } else {
                console.error(`[VU ${vuid}, Iter ${iter}] ${plan.label} failed: ${validateRes.status} - ${validateRes.body}`);
            }
        });
    }

    sleep(1);
}

export function teardown(data) {
    console.log('');
    console.log('='.repeat(60));
    console.log('SMOKE TEST COMPLETE');
    console.log('='.repeat(60));
}

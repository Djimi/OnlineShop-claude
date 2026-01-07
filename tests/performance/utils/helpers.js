/**
 * Helper utilities for Auth Service performance tests
 */

import http from 'k6/http';
import { check } from 'k6';

/**
 * Common HTTP headers for JSON requests
 */
export const JSON_HEADERS = {
    'Content-Type': 'application/json',
};

/**
 * Perform login and return the response
 *
 * @param {string} baseUrl - The base URL of the auth service
 * @param {string} username - Username to login with
 * @param {string} password - Password to login with
 * @returns {object} - The HTTP response object
 */
export function login(baseUrl, username, password) {
    const payload = JSON.stringify({
        username: username,
        password: password,
    });

    return http.post(
        `${baseUrl}/api/v1/auth/login`,
        payload,
        {
            headers: JSON_HEADERS,
            tags: { operation: 'login' },
            responseCallback: http.expectedStatuses(200),
        }
    );
}

/**
 * Perform registration and return the response
 *
 * @param {string} baseUrl - The base URL of the auth service
 * @param {string} username - Username to register
 * @param {string} password - Password to register with
 * @returns {object} - The HTTP response object
 */
export function register(baseUrl, username, password) {
    const payload = JSON.stringify({
        username: username,
        password: password,
    });

    return http.post(
        `${baseUrl}/api/v1/auth/register`,
        payload,
        {
            headers: JSON_HEADERS,
            tags: { operation: 'register' },
            responseCallback: http.expectedStatuses(201, 409),
        }
    );
}

/**
 * Validate a token and return the response
 *
 * @param {string} baseUrl - The base URL of the auth service
 * @param {string} token - The bearer token to validate
 * @returns {object} - The HTTP response object
 */
export function validateToken(baseUrl, token) {
    return http.get(
        `${baseUrl}/api/v1/auth/validate`,
        {
            headers: {
                'Authorization': `Bearer: ${token}`,
            },
            tags: { operation: 'validate' },
            responseCallback: http.expectedStatuses(200),
        }
    );
}

/**
 * Extract token from login response
 *
 * @param {object} response - The HTTP response from login
 * @returns {string|null} - The token or null if not found
 */
export function extractToken(response) {
    try {
        if (response.status === 200) {
            const body = JSON.parse(response.body);
            return body.token || null;
        }
    } catch (e) {
        console.error(`Failed to extract token: ${e.message}`);
    }
    return null;
}

/**
 * Generate a unique username for registration tests
 *
 * @param {string} prefix - Prefix for the username
 * @returns {string} - Unique username
 */
export function generateUniqueUsername(prefix = 'perf_user') {
    const timestamp = Date.now();
    const random = Math.random().toString(36).substring(2, 8);
    return `${prefix}_${timestamp}_${random}`;
}

/**
 * Check login response and return success status
 *
 * @param {object} response - The HTTP response
 * @param {string} context - Context string for check messages
 * @param {object} tags - Tags for the check (for threshold filtering)
 * @returns {boolean} - Whether all checks passed
 */
export function checkLoginResponse(response, context = 'login', tags = { operation: 'login' }) {
    return check(response, {
        [context]: (r) => {
            if (r.status !== 200) return false;
            try {
                const body = JSON.parse(r.body);
                return body.token !== undefined && body.token !== null;
            } catch {
                return false;
            }
        },
    }, tags);
}

/**
 * Check validate response and return success status
 *
 * @param {object} response - The HTTP response
 * @param {string} context - Context string for check messages
 * @param {object} tags - Tags for the check (for threshold filtering)
 * @returns {boolean} - Whether all checks passed
 */
export function checkValidateResponse(response, context = 'validate', tags = { operation: 'validate' }) {
    return check(response, {
        [context]: (r) => r.status === 200,
    }, tags);
}

/**
 * Check register response and return success status
 *
 * @param {object} response - The HTTP response
 * @param {string} context - Context string for check messages
 * @param {object} tags - Tags for the check (for threshold filtering)
 * @returns {boolean} - Whether all checks passed
 */
export function checkRegisterResponse(response, context = 'register', tags = { operation: 'register' }) {
    return check(response, {
        [context]: (r) => r.status === 201 || r.status === 409,
    }, tags);
}

/**
 * Random integer between min and max (inclusive)
 *
 * @param {number} min - Minimum value
 * @param {number} max - Maximum value
 * @returns {number} - Random integer
 */
export function randomIntBetween(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * Pick a random item from an array
 *
 * @param {array} arr - Array to pick from
 * @returns {*} - Random item from array
 */
export function randomItem(arr) {
    return arr[Math.floor(Math.random() * arr.length)];
}

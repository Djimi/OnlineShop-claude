/**
 * Custom metrics for Auth Service performance tests
 *
 * Duration and error rates are tracked automatically via k6's built-in
 * http_req_duration and http_req_failed metrics using operation tags.
 *
 * This module provides additional counters for detailed observability.
 */

import { Counter } from 'k6/metrics';

// Success/failure counters per operation (for observability)
export const successfulLogins = new Counter('auth_successful_logins');
export const failedLogins = new Counter('auth_failed_logins');
export const successfulValidations = new Counter('auth_successful_validations');
export const failedValidations = new Counter('auth_failed_validations');
export const successfulRegistrations = new Counter('auth_successful_registrations');
export const failedRegistrations = new Counter('auth_failed_registrations');

/**
 * Record login metrics (counters only - duration/errors tracked via tags)
 *
 * @param {object} response - HTTP response from login
 * @param {boolean} success - Whether the login was successful
 * @param {object} tags - Additional tags for counters
 */
export function recordLoginMetrics(_response, success, tags = {}) {
    if (success) {
        successfulLogins.add(1, tags);
    } else {
        failedLogins.add(1, tags);
    }
}

/**
 * Record validate metrics (counters only - duration/errors tracked via tags)
 *
 * @param {object} response - HTTP response from validate
 * @param {boolean} success - Whether validation was successful
 * @param {object} tags - Additional tags for counters
 */
export function recordValidateMetrics(_response, success, tags = {}) {
    if (success) {
        successfulValidations.add(1, tags);
    } else {
        failedValidations.add(1, tags);
    }
}

/**
 * Record register metrics (counters only - duration/errors tracked via tags)
 *
 * @param {object} response - HTTP response from register
 * @param {boolean} success - Whether registration was successful
 * @param {object} tags - Additional tags for counters
 */
export function recordRegisterMetrics(_response, success, tags = {}) {
    if (success) {
        successfulRegistrations.add(1, tags);
    } else {
        failedRegistrations.add(1, tags);
    }
}

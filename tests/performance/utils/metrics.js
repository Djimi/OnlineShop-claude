/**
 * Custom metrics for Auth Service performance tests
 *
 * This module uses k6's tagging feature to track operation-specific metrics.
 * Tags allow us to:
 * - Set different thresholds per operation (e.g., 'errors{operation:login}')
 * - Analyze metrics by operation type in the output
 * - Keep the metric structure clean and flexible
 */

import { Rate, Trend, Counter } from 'k6/metrics';

// Error tracking (tagged by operation)
export const errorRate = new Rate('errors');

// Duration trends for each endpoint
export const loginDuration = new Trend('auth_login_duration');
export const validateDuration = new Trend('auth_validate_duration');
export const registerDuration = new Trend('auth_register_duration');

// Success/failure counters per operation
export const successfulLogins = new Counter('auth_successful_logins');
export const failedLogins = new Counter('auth_failed_logins');
export const successfulValidations = new Counter('auth_successful_validations');
export const failedValidations = new Counter('auth_failed_validations');
export const successfulRegistrations = new Counter('auth_successful_registrations');
export const failedRegistrations = new Counter('auth_failed_registrations');

// Total request counters (useful for calculating rates)
export const totalLoginRequests = new Counter('auth_total_login_requests');
export const totalValidateRequests = new Counter('auth_total_validate_requests');
export const totalRegisterRequests = new Counter('auth_total_register_requests');

/**
 * Record login metrics
 *
 * Tracks: duration, errors (tagged), success/failure counts, total requests
 *
 * @param {object} response - HTTP response from login
 * @param {boolean} success - Whether the login was successful
 * @param {object} tags - Additional tags (e.g., { vuid: 1 })
 */
export function recordLoginMetrics(response, success, tags = {}) {
    const metricTags = { operation: 'login', ...tags };

    // Track duration
    loginDuration.add(response.timings.duration, metricTags);

    // Track errors with operation tag
    errorRate.add(!success, metricTags);

    // Track success/failure
    if (success) {
        successfulLogins.add(1, metricTags);
    } else {
        failedLogins.add(1, metricTags);
    }

    // Track total requests
    totalLoginRequests.add(1, metricTags);
}

/**
 * Record validate metrics
 *
 * Tracks: duration, errors (tagged), success/failure counts, total requests
 *
 * @param {object} response - HTTP response from validate
 * @param {boolean} success - Whether validation was successful
 * @param {object} tags - Additional tags (e.g., { vuid: 1 })
 */
export function recordValidateMetrics(response, success, tags = {}) {
    const metricTags = { operation: 'validate', ...tags };

    // Track duration
    validateDuration.add(response.timings.duration, metricTags);

    // Track errors with operation tag
    errorRate.add(!success, metricTags);

    // Track success/failure
    if (success) {
        successfulValidations.add(1, metricTags);
    } else {
        failedValidations.add(1, metricTags);
    }

    // Track total requests
    totalValidateRequests.add(1, metricTags);
}

/**
 * Record register metrics
 *
 * Tracks: duration, errors (tagged), success/failure counts, total requests
 *
 * @param {object} response - HTTP response from register
 * @param {boolean} success - Whether registration was successful
 * @param {object} tags - Additional tags (e.g., { vuid: 1 })
 */
export function recordRegisterMetrics(response, success, tags = {}) {
    const metricTags = { operation: 'register', ...tags };

    // Track duration
    registerDuration.add(response.timings.duration, metricTags);

    // Track errors with operation tag
    errorRate.add(!success, metricTags);

    // Track success/failure
    if (success) {
        successfulRegistrations.add(1, metricTags);
    } else {
        failedRegistrations.add(1, metricTags);
    }

    // Track total requests
    totalRegisterRequests.add(1, metricTags);
}

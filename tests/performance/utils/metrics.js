/**
 * Custom metrics for Auth Service performance tests
 */

import { Rate, Trend, Counter } from 'k6/metrics';

// Error tracking
export const errorRate = new Rate('errors');

// Duration trends for each endpoint
export const loginDuration = new Trend('auth_login_duration');
export const validateDuration = new Trend('auth_validate_duration');
export const registerDuration = new Trend('auth_register_duration');

// Success/failure counters
export const successfulLogins = new Counter('auth_successful_logins');
export const failedLogins = new Counter('auth_failed_logins');
export const successfulValidations = new Counter('auth_successful_validations');
export const failedValidations = new Counter('auth_failed_validations');
export const successfulRegistrations = new Counter('auth_successful_registrations');
export const failedRegistrations = new Counter('auth_failed_registrations');

/**
 * Record login metrics
 *
 * @param {object} response - HTTP response from login
 * @param {boolean} success - Whether the login was successful
 */
export function recordLoginMetrics(response, success) {
    loginDuration.add(response.timings.duration);
    errorRate.add(!success);

    if (success) {
        successfulLogins.add(1);
    } else {
        failedLogins.add(1);
    }
}

/**
 * Record validate metrics
 *
 * @param {object} response - HTTP response from validate
 * @param {boolean} success - Whether validation was successful
 */
export function recordValidateMetrics(response, success) {
    validateDuration.add(response.timings.duration);
    errorRate.add(!success);

    if (success) {
        successfulValidations.add(1);
    } else {
        failedValidations.add(1);
    }
}

/**
 * Record register metrics
 *
 * @param {object} response - HTTP response from register
 * @param {boolean} success - Whether registration was successful
 */
export function recordRegisterMetrics(response, success) {
    registerDuration.add(response.timings.duration);
    errorRate.add(!success);

    if (success) {
        successfulRegistrations.add(1);
    } else {
        failedRegistrations.add(1);
    }
}

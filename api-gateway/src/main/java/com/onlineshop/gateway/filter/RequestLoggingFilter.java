package com.onlineshop.gateway.filter;

import com.onlineshop.gateway.metrics.GatewayMetrics;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Comprehensive request/response logging filter for debugging and observability.
 *
 * Features:
 * - Request timing with Micrometer metrics
 * - Detailed request/response logging (configurable)
 * - Request ID propagation
 * - Structured logging format
 * - Sensitive data masking
 */
@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 2) // After auth and rate limit filters
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final String REQUEST_ID_HEADER = "X-Request-Id";
    private static final String REQUEST_START_TIME_ATTR = "requestStartTime";
    private static final int MAX_PAYLOAD_LENGTH = 10000;

    private final MeterRegistry meterRegistry;

    @Value("${gateway.logging.enabled:true}")
    private boolean loggingEnabled;

    @Value("${gateway.logging.include-headers:false}")
    private boolean includeHeaders;

    @Value("${gateway.logging.include-payload:false}")
    private boolean includePayload;

    @Value("${gateway.logging.include-query-string:true}")
    private boolean includeQueryString;

    @Value("${gateway.logging.slow-request-threshold-ms:1000}")
    private long slowRequestThresholdMs;

    public RequestLoggingFilter(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (!loggingEnabled || isAsyncDispatch(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        // Skip actuator endpoints to reduce noise
        if (request.getRequestURI().startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Wrap request/response for content caching if payload logging is enabled
        HttpServletRequest requestToUse = includePayload
                ? new ContentCachingRequestWrapper(request, MAX_PAYLOAD_LENGTH)
                : request;
        HttpServletResponse responseToUse = includePayload
                ? new ContentCachingResponseWrapper(response)
                : response;

        // Get or generate request ID
        String requestId = getOrCreateRequestId(request);

        // Record start time
        Instant startTime = Instant.now();
        request.setAttribute(REQUEST_START_TIME_ATTR, startTime);

        // Log incoming request
        logRequest(requestToUse, requestId);

        Timer.Sample timerSample = Timer.start(meterRegistry);

        try {
            filterChain.doFilter(requestToUse, responseToUse);
        } finally {
            // Calculate duration
            Duration duration = Duration.between(startTime, Instant.now());

            // Record metrics
            recordMetrics(timerSample, request, response, duration);

            // Log response
            logResponse(requestToUse, responseToUse, requestId, duration);

            // Copy body to response if using content caching
            if (responseToUse instanceof ContentCachingResponseWrapper wrapper) {
                wrapper.copyBodyToResponse();
            }
        }
    }

    private String getOrCreateRequestId(HttpServletRequest request) {
        String requestId = request.getHeader(REQUEST_ID_HEADER);
        if (requestId == null || requestId.isBlank()) {
            requestId = UUID.randomUUID().toString();
        }
        return requestId;
    }

    private void logRequest(HttpServletRequest request, String requestId) {
        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Incoming request: ")
                .append(request.getMethod())
                .append(" ")
                .append(request.getRequestURI());

        if (includeQueryString && request.getQueryString() != null) {
            logBuilder.append("?").append(maskSensitiveParams(request.getQueryString()));
        }

        logBuilder.append(" | requestId=").append(requestId)
                .append(" | remoteAddr=").append(getClientIp(request));

        if (includeHeaders) {
            logBuilder.append(" | headers=").append(getHeadersAsString(request));
        }

        log.info(logBuilder.toString());

        if (includePayload && request instanceof ContentCachingRequestWrapper wrapper) {
            logRequestBody(wrapper, requestId);
        }
    }

    private void logResponse(HttpServletRequest request, HttpServletResponse response,
                             String requestId, Duration duration) {
        long durationMs = duration.toMillis();
        String logLevel = determineLogLevel(response.getStatus(), durationMs);

        StringBuilder logBuilder = new StringBuilder();
        logBuilder.append("Outgoing response: ")
                .append(request.getMethod())
                .append(" ")
                .append(request.getRequestURI())
                .append(" | status=").append(response.getStatus())
                .append(" | duration=").append(durationMs).append("ms")
                .append(" | requestId=").append(requestId);

        if (durationMs > slowRequestThresholdMs) {
            logBuilder.append(" | SLOW_REQUEST");
        }

        // Log at appropriate level
        switch (logLevel) {
            case "ERROR" -> log.error(logBuilder.toString());
            case "WARN" -> log.warn(logBuilder.toString());
            default -> log.info(logBuilder.toString());
        }

        if (includePayload && response instanceof ContentCachingResponseWrapper wrapper) {
            logResponseBody(wrapper, requestId);
        }
    }

    private void logRequestBody(ContentCachingRequestWrapper request, String requestId) {
        byte[] content = request.getContentAsByteArray();
        if (content.length > 0) {
            String payload = truncatePayload(new String(content, StandardCharsets.UTF_8));
            log.debug("Request body [{}]: {}", requestId, maskSensitiveData(payload));
        }
    }

    private void logResponseBody(ContentCachingResponseWrapper response, String requestId) {
        byte[] content = response.getContentAsByteArray();
        if (content.length > 0) {
            String payload = truncatePayload(new String(content, StandardCharsets.UTF_8));
            log.debug("Response body [{}]: {}", requestId, payload);
        }
    }

    private String getHeadersAsString(HttpServletRequest request) {
        return Collections.list(request.getHeaderNames())
                .stream()
                .map(name -> name + "=" + maskSensitiveHeader(name, request.getHeader(name)))
                .collect(Collectors.joining(", ", "{", "}"));
    }

    private String maskSensitiveHeader(String headerName, String headerValue) {
        String lowerName = headerName.toLowerCase();
        if (lowerName.contains("authorization") ||
            lowerName.contains("cookie") ||
            lowerName.contains("token") ||
            lowerName.contains("api-key") ||
            lowerName.contains("secret")) {
            return "***MASKED***";
        }
        return headerValue;
    }

    private String maskSensitiveParams(String queryString) {
        return queryString
                .replaceAll("(?i)(password|token|api_key|apikey|secret|auth)=([^&]*)", "$1=***MASKED***")
                .replaceAll("(?i)(password|token|api_key|apikey|secret|auth)=([^&]*)$", "$1=***MASKED***");
    }

    private String maskSensitiveData(String payload) {
        return payload
                .replaceAll("(?i)\"password\"\\s*:\\s*\"[^\"]*\"", "\"password\":\"***MASKED***\"")
                .replaceAll("(?i)\"token\"\\s*:\\s*\"[^\"]*\"", "\"token\":\"***MASKED***\"")
                .replaceAll("(?i)\"apiKey\"\\s*:\\s*\"[^\"]*\"", "\"apiKey\":\"***MASKED***\"")
                .replaceAll("(?i)\"secret\"\\s*:\\s*\"[^\"]*\"", "\"secret\":\"***MASKED***\"");
    }

    private String truncatePayload(String payload) {
        if (payload.length() > MAX_PAYLOAD_LENGTH) {
            return payload.substring(0, MAX_PAYLOAD_LENGTH) + "...[TRUNCATED]";
        }
        return payload;
    }

    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return request.getRemoteAddr();
    }

    private String determineLogLevel(int status, long durationMs) {
        if (status >= 500) {
            return "ERROR";
        }
        if (status >= 400 || durationMs > slowRequestThresholdMs) {
            return "WARN";
        }
        return "INFO";
    }

    private void recordMetrics(Timer.Sample sample, HttpServletRequest request,
                               HttpServletResponse response, Duration duration) {
        // Record request duration by path pattern and status
        sample.stop(Timer.builder("gateway.http.requests")
                .tag("method", request.getMethod())
                .tag("uri", normalizeUri(request.getRequestURI()))
                .tag("status", String.valueOf(response.getStatus()))
                .tag("outcome", getOutcome(response.getStatus()))
                .description("HTTP request duration")
                .register(meterRegistry));

        // Record slow requests counter
        if (duration.toMillis() > slowRequestThresholdMs) {
            meterRegistry.counter("gateway.http.slow.requests",
                    "method", request.getMethod(),
                    "uri", normalizeUri(request.getRequestURI())
            ).increment();
        }
    }

    private String normalizeUri(String uri) {
        // Normalize URI patterns to avoid high cardinality
        return uri
                .replaceAll("/\\d+", "/{id}")
                .replaceAll("/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}", "/{uuid}");
    }

    private String getOutcome(int status) {
        if (status < 200) return "INFORMATIONAL";
        if (status < 300) return "SUCCESS";
        if (status < 400) return "REDIRECTION";
        if (status < 500) return "CLIENT_ERROR";
        return "SERVER_ERROR";
    }
}

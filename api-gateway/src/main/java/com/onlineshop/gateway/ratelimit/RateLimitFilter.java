package com.onlineshop.gateway.ratelimit;

import tools.jackson.databind.ObjectMapper;
import com.onlineshop.gateway.dto.ErrorResponse;
import com.onlineshop.gateway.exception.TooManyRequestsException;
import com.onlineshop.gateway.metrics.GatewayMetrics;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private final ObjectMapper objectMapper;
    private final GatewayMetrics metrics;
    private final ProxyManager<String> proxyManager;
    private final int anonymousRequestsPerMinute;
    private final int anonymousBurst;
    private final int authenticatedRequestsPerMinute;
    private final int authenticatedBurst;

    public RateLimitFilter(
            ObjectMapper objectMapper,
            GatewayMetrics metrics,
            ProxyManager<String> proxyManager,
            @Value("${gateway.ratelimit.anonymous.requests-per-minute:60}") int anonymousRequestsPerMinute,
            @Value("${gateway.ratelimit.anonymous.burst:10}") int anonymousBurst,
            @Value("${gateway.ratelimit.authenticated.requests-per-minute:300}") int authenticatedRequestsPerMinute,
            @Value("${gateway.ratelimit.authenticated.burst:50}") int authenticatedBurst) {
        this.objectMapper = objectMapper;
        this.metrics = metrics;
        this.proxyManager = proxyManager;
        this.anonymousRequestsPerMinute = anonymousRequestsPerMinute;
        this.anonymousBurst = anonymousBurst;
        this.authenticatedRequestsPerMinute = authenticatedRequestsPerMinute;
        this.authenticatedBurst = authenticatedBurst;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // Skip rate limiting for auth endpoints and health checks
        if (path.startsWith("/auth") || path.startsWith("/actuator")) {
            filterChain.doFilter(request, response);
            return;
        }

        String userId = request.getHeader("X-User-Id");
        String clientKey = userId != null ? "user:" + userId : "ip:" + getClientIP(request);

        boolean isAuthenticated = userId != null;

        BucketConfiguration config = isAuthenticated
                ? createAuthenticatedConfig()
                : createAnonymousConfig();

        Bucket bucket = proxyManager.builder().build(clientKey, config);

        if (bucket.tryConsume(1)) {
            filterChain.doFilter(request, response);
        } else {
            metrics.incrementRateLimitRejections();
            log.warn("Rate limit exceeded for client: {}", clientKey);
            sendTooManyRequestsResponse(response, "Rate limit exceeded. Please try again later.", path);
        }
    }

    private BucketConfiguration createAnonymousConfig() {
        Bandwidth limit = Bandwidth.classic(
                anonymousRequestsPerMinute,
                Refill.intervally(anonymousRequestsPerMinute, Duration.ofMinutes(1))
        );
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    private BucketConfiguration createAuthenticatedConfig() {
        Bandwidth limit = Bandwidth.classic(
                authenticatedRequestsPerMinute,
                Refill.intervally(authenticatedRequestsPerMinute, Duration.ofMinutes(1))
        );
        return BucketConfiguration.builder().addLimit(limit).build();
    }

    private String getClientIP(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private void sendTooManyRequestsResponse(HttpServletResponse response, String detail, String path)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.tooManyRequests(detail, path);

        response.setStatus(429);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}

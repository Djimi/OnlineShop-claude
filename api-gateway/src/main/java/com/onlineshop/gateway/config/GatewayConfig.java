package com.onlineshop.gateway.config;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.DedupeStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.util.unit.DataSize;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.net.URI;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.filter.FilterFunctions.redirectTo;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.*;

/**
 * Main Gateway routing configuration demonstrating Spring Cloud Gateway features.
 * <p>
 * Features demonstrated:
 * - Request/response header manipulation (add, set, remove, map, dedupe)
 * - Security headers (HSTS, CSP, X-Frame-Options, X-Content-Type-Options, etc.)
 * - Request validation (body size limits, header size limits)
 * - Path manipulation (prefix, strip, rewrite, set)
 * - Query parameter manipulation (add, remove)
 * - Advanced predicates (weight, header, query, cookie, time-based)
 * - Redirects (301 Moved Permanently, 302 Found)
 * - Custom fallback responses (404 with RFC 9457 format)
 *
 * @see BodyModificationConfig for body transformation routes
 */
@Configuration
public class GatewayConfig {

    @Value("${gateway.auth.service-url:http://localhost:9001}")
    private String authServiceUrl;

    @Value("${gateway.items.service-url:http://localhost:9000}")
    private String itemsServiceUrl;

    @Value("${gateway.request.max-size:5242880}")
    private long maxRequestSize;

    @Value("${gateway.request.max-header-size:16384}")
    private long maxHeaderSize;

    @Value("${gateway.security.headers.enabled:true}")
    private boolean securityHeadersEnabled;

    @Value("${gateway.security.headers.hsts-max-age:31536000}")
    private int hstsMaxAge;

    @Value("${gateway.security.headers.content-security-policy:default-src 'self'}")
    private String contentSecurityPolicy;

    @Value("${gateway.routing.canary-percentage:10}")
    private int canaryPercentage;

    @Value("${gateway.routing.weight-routing-enabled:true}")
    private boolean weightRoutingEnabled;

    @Bean
    public ObjectMapper objectMapper() {
        return JsonMapper.builder().build();
    }

    // ==========================================
    // MAIN SERVICE ROUTES
    // ==========================================

    /**
     * Auth service route with full header manipulation and security headers.
     */
    @Bean
    @Order(1)
    public RouterFunction<ServerResponse> authRoute() {
        return route("auth-service")
                .route(path("/auth/**"), http())
                .before(uri(authServiceUrl))
                .before(rewritePath("/auth(?<segment>/?.*)", "/api/v1/auth${segment}"))
                .before(preserveHostHeader())
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeadersIfNotPresent("Accept-Language:en-US"))
                .before(addRequestHeadersIfNotPresent("X-Client-Version:1.0.0"))
                .before(removeRequestHeader("X-Debug"))
                .before(removeRequestHeader("X-Trace-Id"))
                // Note: removeResponseHeader and dedupeResponseHeader are disabled due to a bug
                // in Spring Cloud Gateway 5.0.0 that tries to modify read-only headers
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Items service route with request validation, header mapping, and security headers.
     */
    @Bean
    @Order(2)
    public RouterFunction<ServerResponse> itemsRoute() {
        return route("items-service")
                .route(path("/items/**"), http())
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(preserveHostHeader())
                .before(requestSize(DataSize.ofBytes(maxRequestSize)))
                .before(requestHeaderSize(DataSize.ofBytes(maxHeaderSize)))
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeadersIfNotPresent("Accept-Language:en-US"))
                .before(addRequestHeadersIfNotPresent("X-Client-Version:1.0.0"))
                .before(setRequestHeader("X-Security-Level", "standard"))
                .before(mapRequestHeader("User-Agent", "X-Original-User-Agent"))
                .before(removeRequestHeader("X-Debug"))
                .before(removeRequestHeader("X-Trace-Id"))
                .after(addResponseHeader("X-Served-By", "onlineshop-gateway"))
                .filter(securityHeadersFilter())
                .build();
    }

    // ==========================================
    // ADVANCED PREDICATE ROUTES
    // ==========================================

    /**
     * Route for API version 2 requests identified by header.
     * Demonstrates header + method predicates.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsGetVersionedRoute() {
        return route("items-get-versioned")
                .route(
                    path("/items/**").and(method(HttpMethod.GET)).and(header("X-API-Version", "v2")),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeader("X-Routed-By", "header-predicate"))
                .before(addRequestHeader("X-API-Version-Validated", "true"))
                .after(addResponseHeader("X-API-Version", "v2"))
                .after(addResponseHeader("X-Route-Type", "versioned"))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Route for search requests with query parameter predicate.
     * Matches /items/search?q=...
     */
    @Bean
    public RouterFunction<ServerResponse> itemsSearchRoute() {
        return route("items-search")
                .route(
                    path("/items/search").and(query("q")),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items/search", "/api/v1/items/search"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestParameter("limit", "20"))
                .before(addRequestParameter("offset", "0"))
                .after(addResponseHeader("X-Search-Route", "true"))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Route for preview mode based on cookie.
     * Demonstrates cookie predicate.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsPreviewRoute() {
        return route("items-preview")
                .route(
                    path("/items/**").and(cookie("preview_mode", "true")),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeader("X-Preview-Mode", "true"))
                .before(addRequestHeader("X-Feature-Flags", "preview,experimental"))
                .after(addResponseHeader("X-Preview-Mode", "true"))
                .after(addResponseHeader("X-Preview-Notice", "This data is for preview purposes only"))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Canary route for A/B testing (10% of traffic).
     * Demonstrates weight predicate.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsCanaryRoute() {
        if (!weightRoutingEnabled) {
            return route("items-canary-disabled")
                    .route(path("/never-match-disabled"), http())
                    .build();
        }
        return route("items-canary")
                .route(
                    path("/items/**").and(weight("items-group", canaryPercentage)),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeader("X-Canary", "true"))
                .before(addRequestHeader("X-Traffic-Group", "canary"))
                .after(addResponseHeader("X-Canary", "true"))
                .after(addResponseHeader("X-Traffic-Percentage", String.valueOf(canaryPercentage)))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Stable route for A/B testing (90% of traffic).
     * Demonstrates weight predicate.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsStableRoute() {
        if (!weightRoutingEnabled) {
            return route("items-stable-disabled")
                    .route(path("/never-match-disabled"), http())
                    .build();
        }
        return route("items-stable")
                .route(
                    path("/items/**").and(weight("items-group", 100 - canaryPercentage)),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestHeader("X-Canary", "false"))
                .before(addRequestHeader("X-Traffic-Group", "stable"))
                .after(addResponseHeader("X-Canary", "false"))
                .after(addResponseHeader("X-Traffic-Percentage", String.valueOf(100 - canaryPercentage)))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Maintenance window route - active during specific hours.
     * Demonstrates time-based predicate (between).
     * Maintenance window: 2:00 AM - 4:00 AM UTC (configurable).
     */
    @Bean
    public RouterFunction<ServerResponse> maintenanceWindowRoute() {
        return route("maintenance-window")
                .route(path("/items/**").and(request -> isMaintenanceWindow()), request ->
                    ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                        .header("Content-Type", "application/problem+json")
                        .header("Retry-After", "7200")
                        .header("X-Request-Id", getOrCreateRequestId(request))
                        .body("""
                            {
                                "type": "https://api.onlineshop.com/errors/maintenance",
                                "title": "Service Under Maintenance",
                                "status": 503,
                                "detail": "The service is currently under scheduled maintenance. Please try again later.",
                                "instance": "%s",
                                "maintenanceWindow": {
                                    "start": "02:00 UTC",
                                    "end": "04:00 UTC"
                                }
                            }
                            """.formatted(request.path()))
                )
                .build();
    }

    // ==========================================
    // REDIRECT ROUTES
    // ==========================================

    /**
     * Legacy items redirect (301 Moved Permanently).
     */
    @Bean
    public RouterFunction<ServerResponse> legacyItemsRedirectRoute() {
        return route("legacy-items-redirect")
                .route(path("/api/items/**"), http())
                .filter(redirectTo(HttpStatus.MOVED_PERMANENTLY, URI.create("/items")))
                .build();
    }

    /**
     * Legacy auth redirect (301 Moved Permanently).
     */
    @Bean
    public RouterFunction<ServerResponse> legacyAuthRedirectRoute() {
        return route("legacy-auth-redirect")
                .route(path("/api/auth/**"), http())
                .filter(redirectTo(HttpStatus.MOVED_PERMANENTLY, URI.create("/auth")))
                .build();
    }

    /**
     * Documentation redirect (302 Found).
     */
    @Bean
    public RouterFunction<ServerResponse> docsRedirectRoute() {
        return route("docs-redirect")
                .route(path("/docs").or(path("/documentation")), http())
                .filter(redirectTo(HttpStatus.FOUND, URI.create("/swagger-ui.html")))
                .build();
    }

    // ==========================================
    // PATH MANIPULATION ROUTES
    // ==========================================

    /**
     * Strip prefix demo - removes /gateway prefix.
     */
    @Bean
    public RouterFunction<ServerResponse> stripPrefixDemoRoute() {
        return route("strip-prefix-demo")
                .route(path("/gateway/items/**"), http())
                .before(uri(itemsServiceUrl))
                .before(stripPrefix(1))
                .before(rewritePath("/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .after(addResponseHeader("X-Path-Manipulation", "strip-prefix"))
                .filter(securityHeadersFilter())
                .build();
    }

    /**
     * Prefix path demo - adds /api prefix.
     */
    @Bean
    public RouterFunction<ServerResponse> prefixPathDemoRoute() {
        return route("prefix-path-demo")
                .route(path("/v1/items/**"), http())
                .before(uri(itemsServiceUrl))
                .before(prefixPath("/api"))
                .before(request -> addTracingHeaders(request))
                .after(addResponseHeader("X-Path-Manipulation", "prefix-path"))
                .filter(securityHeadersFilter())
                .build();
    }

    // ==========================================
    // QUERY PARAMETER MANIPULATION ROUTES
    // ==========================================

    /**
     * Items list with default query parameters.
     * Adds sorting and pagination, removes debug params.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsListWithDefaultsRoute() {
        return route("items-list-with-defaults")
                .route(
                    path("/items").and(method(HttpMethod.GET)),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/items", "/api/v1/items"))
                .before(request -> addTracingHeaders(request))
                .before(addRequestParameter("sort", "name"))
                .before(addRequestParameter("order", "asc"))
                .before(removeRequestParameter("debug"))
                .before(removeRequestParameter("trace"))
                .after(addResponseHeader("X-Default-Params-Applied", "true"))
                .filter(securityHeadersFilter())
                .build();
    }

    // ==========================================
    // UTILITY ROUTES
    // ==========================================

    /**
     * Gateway health check endpoint.
     */
    @Bean
    public RouterFunction<ServerResponse> gatewayHealthRoute() {
        return route("gateway-health")
                .route(path("/health"), request ->
                    ServerResponse.ok()
                        .header("Content-Type", "application/json")
                        .header("X-Request-Id", getOrCreateRequestId(request))
                        .body("""
                            {
                                "status": "UP",
                                "gateway": "onlineshop-gateway",
                                "version": "1.0.0",
                                "timestamp": "%s"
                            }
                            """.formatted(Instant.now().toString()))
                )
                .build();
    }

    /**
     * Secure items route - removes sensitive headers.
     * Demonstrates header removal for security.
     */
    @Bean
    public RouterFunction<ServerResponse> secureItemsRoute() {
        return route("secure-items")
                .route(
                    path("/secure/items/**").and(header("X-Secure-Access", "true")),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/secure/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addTracingHeaders(request))
                .before(removeRequestHeader("X-Forwarded-For"))
                .before(removeRequestHeader("X-Real-IP"))
                .before(removeRequestHeader("Cookie"))
                .before(addRequestHeader("X-Security-Mode", "strict"))
                // Note: removeResponseHeader("Set-Cookie") disabled due to Spring Cloud Gateway bug
                .after(addResponseHeader("X-Secure-Route", "true"))
                .filter(securityHeadersFilter())
                .build();
    }

    // ==========================================
    // FALLBACK ROUTE
    // ==========================================

    /**
     * Fallback route for unmatched paths.
     * Returns RFC 9457 Problem Details format.
     */
    @Bean
    @Order(Integer.MAX_VALUE)
    public RouterFunction<ServerResponse> fallbackRoute() {
        return route("fallback")
                .route(path("/**"), request ->
                    ServerResponse.status(HttpStatus.NOT_FOUND)
                        .header("Content-Type", "application/problem+json")
                        .header("X-Request-Id", getOrCreateRequestId(request))
                        .body("""
                            {
                                "type": "https://api.onlineshop.com/errors/not-found",
                                "title": "Resource Not Found",
                                "status": 404,
                                "detail": "The requested resource '%s' was not found on this server.",
                                "instance": "%s"
                            }
                            """.formatted(request.path(), request.path()))
                )
                .build();
    }

    // ==========================================
    // FILTER FUNCTIONS
    // ==========================================

    /**
     * Security headers filter - adds comprehensive security headers to all responses.
     */
    private HandlerFilterFunction<ServerResponse, ServerResponse> securityHeadersFilter() {
        return (request, next) -> {
            ServerResponse response = next.handle(request);
            if (securityHeadersEnabled) {
                return ServerResponse.from(response)
                        .header("X-Content-Type-Options", "nosniff")
                        .header("X-Frame-Options", "DENY")
                        .header("X-XSS-Protection", "1; mode=block")
                        .header("Referrer-Policy", "strict-origin-when-cross-origin")
                        .header("Content-Security-Policy", contentSecurityPolicy)
                        .header("Permissions-Policy", "geolocation=(), microphone=(), camera=()")
                        .header("Strict-Transport-Security", "max-age=" + hstsMaxAge + "; includeSubDomains")
                        .header("Cache-Control", "no-store, no-cache, must-revalidate, proxy-revalidate")
                        .header("Pragma", "no-cache")
                        .build();
            }
            return response;
        };
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ServerRequest addTracingHeaders(ServerRequest request) {
        String requestId = getOrCreateRequestId(request);
        return ServerRequest.from(request)
                .header("X-Request-Id", requestId)
                .header("X-Correlation-Id", requestId)
                .header("X-Gateway-Name", "onlineshop-gateway")
                .header("X-Gateway-Timestamp", Instant.now().toString())
                .build();
    }

    private String getOrCreateRequestId(ServerRequest request) {
        String requestId = request.headers().firstHeader("X-Request-Id");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    /**
     * Check if current time is within maintenance window (2:00 AM - 4:00 AM UTC).
     */
    private boolean isMaintenanceWindow() {
        LocalTime now = LocalTime.now(ZoneId.of("UTC"));
        LocalTime start = LocalTime.of(2, 0);
        LocalTime end = LocalTime.of(4, 0);
        return !now.isBefore(start) && now.isBefore(end);
    }
}

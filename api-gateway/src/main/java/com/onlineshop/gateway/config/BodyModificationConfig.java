package com.onlineshop.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Instant;
import java.util.UUID;

import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.filter.AfterFilterFunctions.*;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.predicate.GatewayRequestPredicates.*;

/**
 * Configuration demonstrating additional routing patterns.
 *
 * Note: Body modification (ModifyRequestBody/ModifyResponseBody) requires specific
 * Spring Cloud Gateway MVC configuration patterns. These routes demonstrate
 * alternative approaches using custom handlers.
 *
 * Features demonstrated:
 * - Custom response handlers
 * - Request enrichment via headers
 * - Audit trail injection via headers
 * - Preview/staging environment routing
 */
@Configuration
public class BodyModificationConfig {

    @Value("${gateway.items.service-url:http://localhost:9000}")
    private String itemsServiceUrl;

    @Value("${gateway.auth.service-url:http://localhost:9001}")
    private String authServiceUrl;

    /**
     * Route that adds metadata headers for downstream processing.
     * Instead of modifying request body, we add headers that backends can use.
     *
     * Headers added:
     * - X-Gateway-Metadata-Timestamp
     * - X-Gateway-Metadata-RequestId
     * - X-Gateway-Metadata-Source
     */
    @Bean
    public RouterFunction<ServerResponse> itemsWithMetadataHeadersRoute() {
        return route("items-with-metadata-headers")
                .route(
                    path("/api/v1/enriched/items/**").and(method(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/api/v1/enriched/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> enrichRequestWithMetadata(request))
                .after(addResponseHeader("X-Enriched", "true"))
                .build();
    }

    /**
     * Route that adds audit headers for compliance tracking.
     *
     * Headers added:
     * - X-Audit-Timestamp
     * - X-Audit-User-Id
     * - X-Audit-Client-Ip
     * - X-Audit-Request-Id
     */
    @Bean
    public RouterFunction<ServerResponse> itemsWithAuditHeadersRoute() {
        return route("items-with-audit-headers")
                .route(
                    path("/api/v1/audited/items/**").and(method(HttpMethod.POST, HttpMethod.PUT, HttpMethod.DELETE)),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/api/v1/audited/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(request -> addAuditHeaders(request))
                .after(addResponseHeader("X-Audit-Applied", "true"))
                .build();
    }

    /**
     * Route for staging/preview environment.
     * Adds headers to indicate preview mode.
     */
    @Bean
    public RouterFunction<ServerResponse> stagingItemsRoute() {
        return route("staging-items")
                .route(
                    path("/staging/items/**"),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/staging/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(addRequestHeader("X-Environment", "staging"))
                .before(addRequestHeader("X-Feature-Flags", "preview,experimental"))
                .before(request -> addRequestIdHeader(request))
                .after(addResponseHeader("X-Environment", "staging"))
                .after(addResponseHeader("X-Staging-Warning", "This is staging data - do not use in production"))
                .build();
    }

    /**
     * Route demonstrating response wrapping via custom handler.
     * Returns a wrapped response with metadata.
     */
    @Bean
    public RouterFunction<ServerResponse> apiInfoRoute() {
        return route("api-info")
                .route(path("/api/info"), request ->
                    ServerResponse.ok()
                        .header("Content-Type", "application/json")
                        .header("X-Request-Id", getOrCreateRequestId(request))
                        .body("""
                            {
                                "success": true,
                                "data": {
                                    "gateway": "onlineshop-gateway",
                                    "version": "1.0.0",
                                    "features": [
                                        "authentication",
                                        "rate-limiting",
                                        "caching",
                                        "circuit-breaker",
                                        "request-logging",
                                        "security-headers"
                                    ]
                                },
                                "meta": {
                                    "timestamp": "%s",
                                    "requestId": "%s"
                                }
                            }
                            """.formatted(
                                Instant.now().toString(),
                                getOrCreateRequestId(request)
                            ))
                )
                .build();
    }

    /**
     * Route demonstrating error response formatting.
     * Returns RFC 9457 Problem Details format.
     */
    @Bean
    public RouterFunction<ServerResponse> deprecatedEndpointRoute() {
        return route("deprecated-endpoint")
                .route(path("/api/deprecated/**"), request ->
                    ServerResponse.status(HttpStatus.GONE)
                        .header("Content-Type", "application/problem+json")
                        .header("X-Request-Id", getOrCreateRequestId(request))
                        .body("""
                            {
                                "type": "https://api.onlineshop.com/errors/deprecated",
                                "title": "Endpoint Deprecated",
                                "status": 410,
                                "detail": "This endpoint has been deprecated and removed. Please use the new API.",
                                "instance": "%s"
                            }
                            """.formatted(request.path()))
                )
                .build();
    }

    /**
     * Route for API versioning demonstration.
     * Shows how to handle multiple API versions.
     */
    @Bean
    public RouterFunction<ServerResponse> itemsV2Route() {
        return route("items-v2")
                .route(
                    path("/v2/items/**"),
                    http()
                )
                .before(uri(itemsServiceUrl))
                .before(rewritePath("/v2/items(?<segment>/?.*)", "/api/v1/items${segment}"))
                .before(addRequestHeader("X-API-Version", "v2"))
                .before(addRequestHeader("X-Version-Compatibility", "backward-compatible"))
                .before(request -> addRequestIdHeader(request))
                .after(addResponseHeader("X-API-Version", "v2"))
                .after(addResponseHeader("Deprecation", "false"))
                .build();
    }

    // ==========================================
    // HELPER METHODS
    // ==========================================

    private ServerRequest addRequestIdHeader(ServerRequest request) {
        String requestId = getOrCreateRequestId(request);
        return ServerRequest.from(request)
                .header("X-Request-Id", requestId)
                .header("X-Correlation-Id", requestId)
                .build();
    }

    private String getOrCreateRequestId(ServerRequest request) {
        String requestId = request.headers().firstHeader("X-Request-Id");
        return requestId != null ? requestId : UUID.randomUUID().toString();
    }

    private ServerRequest enrichRequestWithMetadata(ServerRequest request) {
        String requestId = getOrCreateRequestId(request);
        return ServerRequest.from(request)
                .header("X-Request-Id", requestId)
                .header("X-Gateway-Metadata-Timestamp", Instant.now().toString())
                .header("X-Gateway-Metadata-RequestId", requestId)
                .header("X-Gateway-Metadata-Source", "onlineshop-gateway")
                .header("X-Gateway-Metadata-Version", "1.0")
                .build();
    }

    private ServerRequest addAuditHeaders(ServerRequest request) {
        String requestId = getOrCreateRequestId(request);
        String userId = request.headers().firstHeader("X-User-Id");
        String clientIp = getClientIp(request);

        return ServerRequest.from(request)
                .header("X-Request-Id", requestId)
                .header("X-Audit-Timestamp", Instant.now().toString())
                .header("X-Audit-User-Id", userId != null ? userId : "anonymous")
                .header("X-Audit-Client-Ip", clientIp)
                .header("X-Audit-Request-Id", requestId)
                .header("X-Audit-Gateway", "onlineshop-gateway")
                .build();
    }

    private String getClientIp(ServerRequest request) {
        String xForwardedFor = request.headers().firstHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        String xRealIp = request.headers().firstHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp;
        }
        return "unknown";
    }
}

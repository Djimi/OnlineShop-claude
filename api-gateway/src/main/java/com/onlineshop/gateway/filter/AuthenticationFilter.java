package com.onlineshop.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.onlineshop.gateway.dto.ErrorResponse;
import com.onlineshop.gateway.dto.ValidateResponse;
import com.onlineshop.gateway.service.AuthValidationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.*;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AuthenticationFilter extends OncePerRequestFilter {

    private final AuthValidationService authValidationService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer: ";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Skip CORS preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getRequestURI();

        // Skip authentication for auth endpoints
        if (path.startsWith("/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Only authenticate /items/** requests
        if (!path.startsWith("/items")) {
            filterChain.doFilter(request, response);
            return;
        }

        String authHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorizedResponse(response, "Missing or invalid Authorization header", path);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        Optional<ValidateResponse> validationResult = authValidationService.validateToken(token);

        if (validationResult.isEmpty()) {
            sendUnauthorizedResponse(response, "Invalid or expired token", path);
            return;
        }

        ValidateResponse validateResponse = validationResult.get();

        // Add user info headers for downstream services
        HttpServletRequest wrappedRequest = new HeaderMapRequestWrapper(request, Map.of(
                "X-User-Id", validateResponse.getUserId().toString(),
                "X-Username", validateResponse.getUsername()
        ));

        log.debug("Authenticated request for user: {} (ID: {})",
                validateResponse.getUsername(), validateResponse.getUserId());

        filterChain.doFilter(wrappedRequest, response);
    }

    private void sendUnauthorizedResponse(HttpServletResponse response, String detail, String path)
            throws IOException {
        ErrorResponse errorResponse = ErrorResponse.unauthorized(detail, path);

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }

    /**
     * Wrapper to add custom headers to the request for downstream services.
     */
    private static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> additionalHeaders;

        public HeaderMapRequestWrapper(HttpServletRequest request, Map<String, String> additionalHeaders) {
            super(request);
            this.additionalHeaders = additionalHeaders;
        }

        @Override
        public String getHeader(String name) {
            String header = additionalHeaders.get(name);
            return header != null ? header : super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            Set<String> headerNames = new HashSet<>(additionalHeaders.keySet());
            Enumeration<String> originalNames = super.getHeaderNames();
            while (originalNames.hasMoreElements()) {
                headerNames.add(originalNames.nextElement());
            }
            return Collections.enumeration(headerNames);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            String additionalHeader = additionalHeaders.get(name);
            if (additionalHeader != null) {
                return Collections.enumeration(Collections.singletonList(additionalHeader));
            }
            return super.getHeaders(name);
        }
    }
}

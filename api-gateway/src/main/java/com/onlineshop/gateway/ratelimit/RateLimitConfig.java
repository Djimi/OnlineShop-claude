package com.onlineshop.gateway.ratelimit;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "gateway.ratelimit")
public record RateLimitConfig(
        LimitSettings anonymous,
        LimitSettings authenticated
) {
    public record LimitSettings(int requestsPerMinute, int burst) {
    }
}

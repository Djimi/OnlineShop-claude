package com.onlineshop.gateway.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {

    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;

    public static ErrorResponse unauthorized(String detail, String instance) {
        return ErrorResponse.builder()
                .type("https://api.onlineshop.com/errors/unauthorized")
                .title("Unauthorized")
                .status(401)
                .detail(detail)
                .instance(instance)
                .build();
    }

    public static ErrorResponse serviceUnavailable(String detail, String instance) {
        return ErrorResponse.builder()
                .type("https://api.onlineshop.com/errors/service-unavailable")
                .title("Service Unavailable")
                .status(503)
                .detail(detail)
                .instance(instance)
                .build();
    }

    public static ErrorResponse gatewayTimeout(String detail, String instance) {
        return ErrorResponse.builder()
                .type("https://api.onlineshop.com/errors/gateway-timeout")
                .title("Gateway Timeout")
                .status(504)
                .detail(detail)
                .instance(instance)
                .build();
    }

    public static ErrorResponse badGateway(String detail, String instance) {
        return ErrorResponse.builder()
                .type("https://api.onlineshop.com/errors/bad-gateway")
                .title("Bad Gateway")
                .status(502)
                .detail(detail)
                .instance(instance)
                .build();
    }

    public static ErrorResponse tooManyRequests(String detail, String instance) {
        return ErrorResponse.builder()
                .type("https://api.onlineshop.com/errors/too-many-requests")
                .title("Too Many Requests")
                .status(429)
                .detail(detail)
                .instance(instance)
                .build();
    }
}

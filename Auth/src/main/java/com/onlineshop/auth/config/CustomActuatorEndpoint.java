package com.onlineshop.auth.config;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "custom")
public class CustomActuatorEndpoint {

    @ReadOperation
    public String customEndpoint() {
        return "This is a custom endpoint";
    }
}
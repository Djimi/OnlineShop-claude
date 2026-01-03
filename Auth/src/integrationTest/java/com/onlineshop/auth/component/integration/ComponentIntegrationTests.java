package com.onlineshop.auth.component.integration;

import com.onlineshop.auth.BaseIntegrationTest;
import com.onlineshop.auth.dto.RegisterRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.test.web.servlet.client.RestTestClient;

@AutoConfigureRestTestClient
class ComponentIntegrationTests extends BaseIntegrationTest {

    @Autowired
    private RestTestClient restTestClient;

    @Test
    void register_sendsRealHttpRequest_printsResponse() {
        RegisterRequest request = new RegisterRequest("testuser222", "password123");

        var result = restTestClient.post()
                .uri("/api/v1/auth/register")
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(String.class);

        System.out.println("=== Registration Response ===");
        System.out.println("Status: " + result.getStatus());
        System.out.println("Headers: " + result.getResponseHeaders());
        System.out.println("Body: " + result.getResponseBody());
        System.out.println("=============================");
    }
}

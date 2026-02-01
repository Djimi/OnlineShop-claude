package com.onlineshop.gateway.endpoint;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "gateway.ratelimit.enabled=false"
)
class ProductInfoEndpointTest {

    private static final String PATH = "/api/product-info";
    private static final String DESCRIPTION = "This is a test product for learning";

    @LocalServerPort
    private int port;

    @Test
    void getProductInfo_returnsStaticDescription() throws Exception {
        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + PATH))
                .GET()
                .build();

        HttpResponse<String> result = client.send(request, HttpResponse.BodyHandlers.ofString());

        assertThat(result.statusCode()).isEqualTo(HttpStatus.OK.value());

        String contentType = result.headers()
                .firstValue("Content-Type")
                .orElse(null);
        assertThat(contentType).isNotNull();
        assertThat(MediaType.parseMediaType(contentType).isCompatibleWith(MediaType.APPLICATION_JSON)).isTrue();

        String body = result.body();
        assertThat(body).isNotNull();

        JsonNode json = JsonMapper.builder().build().readTree(body);
        assertThat(json.get("description").asText()).isEqualTo(DESCRIPTION);
    }
}

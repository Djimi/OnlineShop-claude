package com.onlineshop.items.web.controller;

import com.onlineshop.items.web.dto.ItemResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@Testcontainers
class ItemsControllerE2eTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:18-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @LocalServerPort
    private int port;

    private RestTemplate restTemplate;
    private String baseUrl;

    @BeforeEach
    void setUp() {
        restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.ResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;
            }
            @Override
            public void handleError(org.springframework.http.client.ClientHttpResponse response) {
            }
        });
        baseUrl = "http://localhost:" + port + "/api/v1/items";
    }

    @Test
    void fullLifecycle() {
        var createPayload = Map.of("name", "E2E Item", "quantity", 42, "description", "E2E desc");
        var createResponse = restTemplate.postForEntity(baseUrl, createPayload, ItemResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody()).isNotNull();
        assertThat(createResponse.getBody().name()).isEqualTo("E2E Item");
        assertThat(createResponse.getBody().quantity()).isEqualTo(42);
        assertThat(createResponse.getBody().description()).isEqualTo("E2E desc");
        UUID id = createResponse.getBody().id();
        assertThat(id).isNotNull();

        var getResponse = restTemplate.getForEntity(baseUrl + "/{id}", ItemResponse.class, id);
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(getResponse.getBody().name()).isEqualTo("E2E Item");

        var updatePayload = Map.of("name", "Updated", "quantity", 10, "description", "Updated desc");
        restTemplate.put(baseUrl + "/{id}", updatePayload, id);

        var updatedResponse = restTemplate.getForEntity(baseUrl + "/{id}", ItemResponse.class, id);
        assertThat(updatedResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(updatedResponse.getBody().name()).isEqualTo("Updated");
        assertThat(updatedResponse.getBody().quantity()).isEqualTo(10);

        restTemplate.delete(baseUrl + "/{id}", id);

        var deletedResponse = restTemplate.getForEntity(baseUrl + "/{id}", String.class, id);
        assertThat(deletedResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createItem_withNullDescription() {
        var createPayload = Map.of("name", "Null Desc", "quantity", 1);
        var createResponse = restTemplate.postForEntity(baseUrl, createPayload, ItemResponse.class);
        assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(createResponse.getBody().description()).isEmpty();
    }

    @Test
    void getAllItems_returnsList() {
        restTemplate.postForEntity(baseUrl, Map.of("name", "Item A", "quantity", 1), ItemResponse.class);
        restTemplate.postForEntity(baseUrl, Map.of("name", "Item B", "quantity", 2), ItemResponse.class);

        var response = restTemplate.getForEntity(baseUrl, ItemResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSizeGreaterThanOrEqualTo(2);
    }

    @Test
    void searchItems_byDescription() {
        restTemplate.postForEntity(baseUrl, Map.of("name", "SearchMe", "quantity", 1, "description", "UniqueDesc"), ItemResponse.class);

        var searchUrl = baseUrl + "/search?description=UniqueDesc";
        var response = restTemplate.getForEntity(searchUrl, ItemResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(1);
        assertThat(response.getBody()[0].name()).isEqualTo("SearchMe");
    }

    @Test
    void getItemById_notFound_returns404() {
        var id = UUID.randomUUID();
        var response = restTemplate.getForEntity(baseUrl + "/{id}", String.class, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void deleteItem_notFound_returns404() {
        var id = UUID.randomUUID();
        var response = restTemplate.exchange(
                RequestEntity.delete(URI.create(baseUrl + "/" + id)).build(),
                String.class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchItems_noMatch_returnsEmptyList() {
        var searchUrl = baseUrl + "/search?description=NonExistentXYZ";
        var response = restTemplate.getForEntity(searchUrl, ItemResponse[].class);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }
}

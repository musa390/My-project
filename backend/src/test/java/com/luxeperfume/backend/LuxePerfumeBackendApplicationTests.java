package com.luxeperfume.backend;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LuxePerfumeBackendApplicationTests {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void actuatorHealthEndpointReportsUp() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/actuator/health", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("status", "UP");
    }

    @Test
    void categoriesEndpointReadsSeededCategoriesFromDatabase() {
        ResponseEntity<Map> response =
                restTemplate.getForEntity("/api/v1/categories", Map.class);

        assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
        assertThat(response.getBody()).containsEntry("success", true);
        assertThat(response.getBody().get("data")).isNotNull();
    }
}

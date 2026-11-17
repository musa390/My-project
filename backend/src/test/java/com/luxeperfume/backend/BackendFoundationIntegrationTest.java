package com.luxeperfume.backend;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.hamcrest.Matchers.containsString;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "management.endpoint.health.show-details=always")
@AutoConfigureMockMvc
class BackendFoundationIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void healthEndpointReturnsUp() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("UP"));
    }

    @Test
    void actuatorReportsDatabaseAsUp() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void openApiDocumentIsAvailable() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("LUXE PERFUME API"))
                .andExpect(jsonPath("$.info.version").value("v1"));
    }

    @Test
    void businessApiRequiresAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/private-test"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminAndStaffRoutesRejectAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/v1/admin/security"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/staff/inventory"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csrfBootstrapIsAvailable() throws Exception {
        mockMvc.perform(get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty());
    }

    @Test
    void registrationHashesPasswordAndSetsHttpOnlyCookies() throws Exception {
        String email = "phase6-" + UUID.randomUUID() + "@example.invalid";
        mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                        .contentType("application/json")
                        .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Test\",\"lastName\":\"Customer\"}"))
                .andExpect(status().isOk())
                .andExpect(header().string("Set-Cookie", containsString("lp_access=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE email=?", String.class, email);
        assertThat(hash).isNotNull().startsWith("$2");
        assertThat(hash).isNotEqualTo("StrongPassphrase123!");
    }

    @Test
    void unsafeCookieRequestsRequireCsrfProtection() throws Exception {
        mockMvc.perform(post("/api/v1/private-test"))
                .andExpect(status().isForbidden());
    }

    @Test
    void existingPhaseFourMigrationsAreApplied() {
        assertThat(flyway.info().applied()).hasSizeGreaterThanOrEqualTo(3);
    }
}

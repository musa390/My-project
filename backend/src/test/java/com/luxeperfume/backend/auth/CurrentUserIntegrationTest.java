package com.luxeperfume.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CurrentUserIntegrationTest {
    @Autowired CurrentUserController controller;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnsOnlySafeProfileFieldsAndAssignedRoles() {
        String email = "phase13-user-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("""
                INSERT INTO users(email,password_hash,first_name,last_name,account_status,email_verified_at)
                VALUES(?, 'not-a-real-password-hash', 'Phase13', 'Customer', 'ACTIVE', UTC_TIMESTAMP())
                """, email);
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='CUSTOMER'", userId);

        Map<String, Object> response = controller.currentUser(new TestingAuthenticationToken(userId, "unused"));
        @SuppressWarnings("unchecked")
        Map<String, Object> user = (Map<String, Object>) response.get("user");

        assertThat(response).containsEntry("success", true);
        assertThat(user).containsEntry("id", userId)
                .containsEntry("email", email)
                .containsEntry("firstName", "Phase13")
                .containsEntry("lastName", "Customer")
                .containsEntry("roles", java.util.List.of("CUSTOMER"));
        assertThat(user).doesNotContainKeys("password", "passwordHash", "password_hash");
    }
}

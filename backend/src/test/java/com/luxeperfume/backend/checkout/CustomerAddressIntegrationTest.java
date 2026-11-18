package com.luxeperfume.backend.checkout;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class CustomerAddressIntegrationTest {
    @Autowired CustomerAddressController controller;
    @Autowired JdbcTemplate jdbc;

    @AfterEach
    void clearSecurityContext() { SecurityContextHolder.clearContext(); }

    @Test
    void customerCanCreateListAndDeleteOnlyTheirOwnAddress() {
        String email = "phase13-address-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("""
                INSERT INTO users(email,password_hash,first_name,last_name,account_status,email_verified_at)
                VALUES(?, 'not-a-real-password-hash', 'Phase13', 'Address', 'ACTIVE', UTC_TIMESTAMP())
                """, email);
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='CUSTOMER'", userId);
        var authentication = new TestingAuthenticationToken(userId, "unused", "ROLE_CUSTOMER");
        SecurityContextHolder.getContext().setAuthentication(authentication);

        var request = new CustomerAddressController.AddressRequest(
                "Home", "Phase13 Customer", "01700000000", "House 10, Road 2",
                "Apartment 4B", "Dhanmondi", "Dhaka", "Dhaka", "1205", "BD", true);

        Map<String,Object> created = controller.create(authentication, request).data();
        long addressId = ((Number) created.get("id")).longValue();
        assertThat(created).containsEntry("recipient_name", "Phase13 Customer")
                .containsEntry("is_default", true);

        List<Map<String,Object>> addresses = controller.list(authentication).data();
        assertThat(addresses).hasSize(1);
        assertThat(addresses.getFirst()).containsEntry("id", addressId);

        controller.delete(authentication, addressId);
        assertThat(controller.list(authentication).data()).isEmpty();
    }
}

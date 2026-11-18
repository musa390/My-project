package com.luxeperfume.backend.contact;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class ContactIntegrationTest {
    @Autowired ContactController contacts;
    @Autowired JdbcTemplate jdbc;

    @Test
    void contactSubmissionPersistsMessageAndCreatesAdminInboxNotification() {
        String adminEmail="phase12-admin-"+UUID.randomUUID()+"@example.invalid";
        jdbc.update("""
            INSERT INTO users(email,password_hash,first_name,account_status,email_verified_at)
            VALUES(?, 'test-hash', 'Phase12 Admin', 'ACTIVE', UTC_TIMESTAMP())
            """,adminEmail);
        Long adminId=jdbc.queryForObject("SELECT id FROM users WHERE email=?",Long.class,adminEmail);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='ADMIN'",adminId);

        MockHttpServletRequest request=new MockHttpServletRequest();
        request.setRemoteAddr("192.0.2.10");
        contacts.submit(new ContactController.ContactRequest("Test Customer","customer@example.invalid",
                "Order question","Please help me understand my order status."),request);

        Long messageId=jdbc.queryForObject("SELECT id FROM contact_messages WHERE email=?",
                Long.class,"customer@example.invalid");
        assertThat(messageId).isNotNull();
        Integer adminNotice=jdbc.queryForObject("""
            SELECT COUNT(*) FROM user_notifications
            WHERE user_id=? AND aggregate_type='CONTACT' AND aggregate_id=?
            """,Integer.class,adminId,messageId);
        assertThat(adminNotice).isEqualTo(1);
    }
}

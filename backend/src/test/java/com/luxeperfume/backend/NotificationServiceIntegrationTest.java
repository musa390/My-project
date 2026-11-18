package com.luxeperfume.backend;

import com.luxeperfume.backend.notification.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class NotificationServiceIntegrationTest {
    @Autowired NotificationService notifications;
    @Autowired JdbcTemplate jdbc;

    @Test
    void orderEventsAreIdempotentAndInboxIsStrictlyUserScoped() {
        long userId = createUser();
        long otherUserId = createUser();
        long orderId = createOrder(userId);

        notifications.orderPlaced(userId, orderId);
        notifications.orderPlaced(userId, orderId);
        notifications.orderStatusChanged(userId, orderId, "CONFIRMED");

        assertThat(notifications.inbox(userId, 0, 20)).hasSize(2);
        assertThat(notifications.inbox(otherUserId, 0, 20)).isEmpty();

        long notificationId = ((Number) notifications.inbox(userId, 0, 20).getLast().get("id")).longValue();
        assertThat(notifications.markRead(userId, notificationId)).isTrue();
        assertThat(notifications.markRead(userId, notificationId)).isTrue();
        assertThat(notifications.markRead(otherUserId, notificationId)).isFalse();
    }

    @Test
    void unsupportedOrderStatusCannotCreateNotification() {
        long userId = createUser();
        long orderId = createOrder(userId);
        assertThatThrownBy(() -> notifications.orderStatusChanged(userId, orderId, "PAID"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(notifications.inbox(userId, 0, 20)).isEmpty();
    }

    private long createUser() {
        String email = "notification-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("""
                INSERT INTO users(email,password_hash,first_name,account_status,email_verified_at)
                VALUES(?, 'test-hash', 'Notification', 'ACTIVE', CURRENT_TIMESTAMP)
                """, email);
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
    }

    private long createOrder(long userId) {
        String number = "NTF-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_orders(order_number,user_id,order_status,payment_status,currency,
                    subtotal,discount_total,shipping_total,tax_total,grand_total,
                    shipping_recipient,shipping_phone,customer_email,shipping_address_line1,
                    shipping_city,shipping_country_code)
                VALUES(?,?,'PENDING','UNPAID','BDT',100,0,0,0,100,
                    'Test Recipient','01700000000','notification@example.invalid','Test Road','Dhaka','BD')
                """, number, userId);
        return jdbc.queryForObject("SELECT id FROM customer_orders WHERE order_number=?", Long.class, number);
    }
}

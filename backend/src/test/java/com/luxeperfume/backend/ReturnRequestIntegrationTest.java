package com.luxeperfume.backend;

import com.luxeperfume.backend.returns.ReturnRequestService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class ReturnRequestIntegrationTest {
    @Autowired ReturnRequestService returns;
    @Autowired JdbcTemplate jdbc;

    @Test
    void returnRequiresDeliveredOrderAndMovesThroughAuditedManualReview() {
        long userId = createUser();
        long orderId = createOrder(userId, "PENDING");
        assertThatThrownBy(() -> returns.requestReturn(userId, orderNumber(orderId),
                "Changed my mind", "return-key-pending"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.CONFLICT));

        jdbc.update("UPDATE customer_orders SET order_status='DELIVERED' WHERE id=?", orderId);
        String orderNumber = orderNumber(orderId);
        var requested = returns.requestReturn(userId, orderNumber, "Changed my mind", "return-key-123");
        var duplicate = returns.requestReturn(userId, orderNumber, "Changed my mind", "return-key-123");
        long returnId = ((Number) requested.get("id")).longValue();

        assertThat(duplicate.get("id")).isEqualTo(requested.get("id"));
        assertThat(requested.get("returnStatus")).isEqualTo("REQUESTED");
        assertThatThrownBy(() -> returns.requestReturn(userId, orderNumber, "Second request", "return-key-456"))
                .isInstanceOf(ResponseStatusException.class);

        var approved = returns.decide(returnId, userId, "APPROVED", "Approved for return");
        assertThat(approved.get("returnStatus")).isEqualTo("APPROVED");
        var received = returns.markReceived(returnId, userId, "Package received");
        assertThat(received.get("returnStatus")).isEqualTo("RECEIVED");
        assertThat(jdbc.queryForObject("SELECT order_status FROM customer_orders WHERE id=?",
                String.class, orderId)).isEqualTo("RETURNED");
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM return_request_events WHERE return_request_id=?",
                Integer.class, returnId)).isEqualTo(3);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM order_status_history
                WHERE order_id=? AND from_status='DELIVERED' AND to_status='RETURNED'
                """, Integer.class, orderId)).isEqualTo(1);
    }

    @Test
    void rejectedReturnCannotBeReceivedAndCustomerCannotSeeAnotherUsersRequest() {
        long ownerId = createUser();
        long otherId = createUser();
        long orderId = createOrder(ownerId, "DELIVERED");
        String orderNumber = orderNumber(orderId);
        var requested = returns.requestReturn(ownerId, orderNumber, "Damaged package", "return-key-789");
        long returnId = ((Number) requested.get("id")).longValue();

        assertThatThrownBy(() -> returns.customerRequests(otherId, orderNumber))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(error -> assertThat(((ResponseStatusException) error).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
        returns.decide(returnId, ownerId, "REJECTED", "Rejected after review");
        assertThatThrownBy(() -> returns.markReceived(returnId, ownerId, "Should not be received"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(jdbc.queryForObject("SELECT order_status FROM customer_orders WHERE id=?",
                String.class, orderId)).isEqualTo("DELIVERED");
    }

    private long createUser() {
        String email = "return-" + UUID.randomUUID() + "@example.invalid";
        jdbc.update("""
                INSERT INTO users(email,password_hash,first_name,account_status,email_verified_at)
                VALUES(?, 'test-hash', 'Return', 'ACTIVE', CURRENT_TIMESTAMP)
                """, email);
        return jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
    }

    private long createOrder(long userId, String status) {
        String number = "RET-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO customer_orders(order_number,user_id,order_status,payment_status,currency,
                    subtotal,discount_total,shipping_total,tax_total,grand_total,
                    shipping_recipient,shipping_phone,customer_email,shipping_address_line1,
                    shipping_city,shipping_country_code)
                VALUES(?,?,?,'UNPAID','BDT',100,0,0,0,100,
                    'Test Recipient','01700000000','return@example.invalid','Test Road','Dhaka','BD')
                """, number, userId, status);
        return jdbc.queryForObject("SELECT id FROM customer_orders WHERE order_number=?", Long.class, number);
    }

    private String orderNumber(long orderId) {
        return jdbc.queryForObject("SELECT order_number FROM customer_orders WHERE id=?", String.class, orderId);
    }
}

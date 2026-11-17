package com.luxeperfume.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentRefundIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void refundRemainsManualReviewUntilReconciledAndUpdatesBothStatuses() throws Exception {
        Session staff = registerManager();
        long orderId = createPaidOrder(staff.userId());
        long paymentId = jdbc.queryForObject(
                "SELECT id FROM payments WHERE order_id=?", Long.class, orderId);

        String firstKey = "refund-" + UUID.randomUUID();
        MvcResult first = mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/refunds")
                .with(csrf()).cookie(staff.access()).header("Idempotency-Key", firstKey)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"40.00\",\"currency\":\"BDT\",\"reason\":\"Customer return approved\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundStatus").value("MANUAL_REVIEW"))
            .andReturn();

        long refundId = ((Number) com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.data.id")).longValue();

        assertThat(jdbc.queryForObject("SELECT payment_status FROM payments WHERE id=?",
                String.class, paymentId)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject("SELECT payment_status FROM customer_orders WHERE id=?",
                String.class, orderId)).isEqualTo("PAID");

        mockMvc.perform(post("/api/v1/staff/refunds/" + refundId + "/reconcile")
                .with(csrf()).cookie(staff.access()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reconciliationReference\":\"BANK-REF-00001\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundStatus").value("SUCCEEDED"));

        assertThat(jdbc.queryForObject("SELECT payment_status FROM payments WHERE id=?",
                String.class, paymentId)).isEqualTo("PARTIALLY_REFUNDED");
        assertThat(jdbc.queryForObject("SELECT payment_status FROM customer_orders WHERE id=?",
                String.class, orderId)).isEqualTo("PARTIALLY_REFUNDED");

        mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/refunds")
                .with(csrf()).cookie(staff.access()).header("Idempotency-Key", "refund-" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"60.00\",\"currency\":\"BDT\",\"reason\":\"Remaining approved refund\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundStatus").value("MANUAL_REVIEW"));

        Long secondRefundId = jdbc.queryForObject(
                "SELECT id FROM payment_refunds WHERE payment_id=? AND amount=60.00",
                Long.class, paymentId);

        mockMvc.perform(post("/api/v1/staff/refunds/" + secondRefundId + "/reconcile")
                .with(csrf()).cookie(staff.access()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"reconciliationReference\":\"BANK-REF-00002\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.refundStatus").value("SUCCEEDED"));

        assertThat(jdbc.queryForObject("SELECT payment_status FROM payments WHERE id=?",
                String.class, paymentId)).isEqualTo("REFUNDED");
        assertThat(jdbc.queryForObject("SELECT payment_status FROM customer_orders WHERE id=?",
                String.class, orderId)).isEqualTo("REFUNDED");
    }

    @Test
    void refundCannotExceedRemainingAmountAndDuplicateRequestIsIdempotent() throws Exception {
        Session staff = registerManager();
        long orderId = createPaidOrder(staff.userId());
        long paymentId = jdbc.queryForObject(
                "SELECT id FROM payments WHERE order_id=?", Long.class, orderId);
        String key = "refund-" + UUID.randomUUID();
        String body = "{\"amount\":\"75.00\",\"currency\":\"BDT\",\"reason\":\"Partial return\"}";

        MvcResult first = mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/refunds")
                .with(csrf()).cookie(staff.access()).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk()).andReturn();
        String firstId = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.data.id").toString();

        mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/refunds")
                .with(csrf()).cookie(staff.access()).header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(Long.parseLong(firstId)));

        mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/refunds")
                .with(csrf()).cookie(staff.access()).header("Idempotency-Key", "refund-" + UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"amount\":\"30.00\",\"currency\":\"BDT\",\"reason\":\"Excess request\"}"))
            .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payment_refunds WHERE payment_id=?", Integer.class, paymentId))
                .isEqualTo(1);
    }

    private Session registerManager() throws Exception {
        String email = "refund-manager-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"" + email
                        + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"RefundManager\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        long roleId = jdbc.queryForObject("SELECT id FROM roles WHERE code='MANAGER'", Long.class);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, roleId);
        return new Session(access, userId);
    }

    private long createPaidOrder(long userId) {
        String orderNumber = "PAYREF-" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        jdbc.update("""
                INSERT INTO customer_orders
                    (order_number,user_id,order_status,payment_status,currency,subtotal,
                     discount_total,shipping_total,tax_total,grand_total,shipping_recipient,
                     shipping_phone,customer_email,shipping_address_line1,shipping_city)
                VALUES(?,?,'DELIVERED','PAID','BDT',100.00,0.00,0.00,0.00,100.00,
                       'Refund Test','01700000000','refund@example.invalid','1 Refund Road','Dhaka')
                """, orderNumber, userId);
        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        jdbc.update("""
                INSERT INTO payments(order_id,provider,payment_method,amount,currency,payment_status)
                VALUES(?,'BKASH','MOBILE_BANKING',100.00,'BDT','PAID')
                """, orderId);
        return orderId;
    }

    private record Session(Cookie access, long userId) {}
}

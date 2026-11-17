package com.luxeperfume.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.security.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void codRemainsPendingUntilStaffConfirmsCollectionAndRetriesAreIdempotent() throws Exception {
        Session customer = register("payment-cod");
        String orderNumber = placeOrder(customer, 1);
        String key = "cod-" + UUID.randomUUID();

        MvcResult first = mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/payments/cod")
                .with(csrf()).cookie(customer.access()).header("Idempotency-Key", key))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.provider").value("COD"))
            .andExpect(jsonPath("$.data.paymentStatus").value("PENDING"))
            .andReturn();

        Number paymentIdValue = com.jayway.jsonpath.JsonPath.read(
                first.getResponse().getContentAsString(), "$.data.id");
        long paymentId = paymentIdValue.longValue();
        assertThat(jdbc.queryForObject(
                "SELECT payment_status FROM customer_orders WHERE order_number=?",
                String.class, orderNumber)).isEqualTo("PENDING");
        assertThat(jdbc.queryForObject(
                "SELECT payment_status FROM payments WHERE id=?", String.class, paymentId))
                .isEqualTo("PENDING");

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/payments/cod")
                .with(csrf()).cookie(customer.access()).header("Idempotency-Key", key))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.id").value(paymentId));
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE order_id=(SELECT id FROM customer_orders WHERE order_number=?)",
                Integer.class, orderNumber)).isEqualTo(1);

        long managerRole = jdbc.queryForObject("SELECT id FROM roles WHERE code='MANAGER'", Long.class);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", customer.userId(), managerRole);
        mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/cod-collection")
                .with(csrf()).cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));

        assertThat(jdbc.queryForObject(
                "SELECT payment_status FROM customer_orders WHERE order_number=?",
                String.class, orderNumber)).isEqualTo("PAID");
        assertThat(jdbc.queryForObject(
                "SELECT payment_status FROM payments WHERE id=?", String.class, paymentId))
                .isEqualTo("PAID");

        mockMvc.perform(post("/api/v1/staff/payments/" + paymentId + "/cod-collection")
                .with(csrf()).cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.paymentStatus").value("PAID"));
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM payment_events
                WHERE payment_id=? AND event_type='COD_COLLECTED'
                """, Integer.class, paymentId)).isEqualTo(1);

        mockMvc.perform(get("/api/v1/orders/" + orderNumber + "/payments").cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].paymentStatus").value("PAID"));
    }

    @Test
    void anotherCustomerCannotInitiateCodForSomeoneElsesOrder() throws Exception {
        Session owner = register("payment-owner");
        Session other = register("payment-other");
        String orderNumber = placeOrder(owner, 1);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/payments/cod")
                .with(csrf()).cookie(other.access())
                .header("Idempotency-Key", "cod-" + UUID.randomUUID()))
            .andExpect(status().isNotFound());
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM payments WHERE order_id=(SELECT id FROM customer_orders WHERE order_number=?)",
                Integer.class, orderNumber)).isZero();
    }

    private String placeOrder(Session customer, int quantity) throws Exception {
        long variantId = createVariant(new BigDecimal("725.00"), 8);
        jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", customer.userId());
        long cartId = jdbc.queryForObject(
                "SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'", Long.class, customer.userId());
        jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)",
                cartId, variantId, quantity);
        long addressId = createAddress(customer.userId());
        MvcResult result = mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json").content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isOk()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),
                "$.data.orderNumber");
    }

    private Session register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email
                        + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"PaymentTest\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        return new Session(access, userId);
    }

    private long createVariant(BigDecimal price, int stock) {
        long categoryId = jdbc.queryForObject(
                "SELECT id FROM categories WHERE slug='floral'", Long.class);
        String slug = "phase10-payment-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO products(category_id,name,slug,product_type,status)
                VALUES(?,? ,?,'VARIABLE','ACTIVE')
                """, categoryId, "Payment Test Product", slug);
        long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug=?", Long.class, slug);
        String sku = "PAY-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_variants(product_id,sku,variant_name,price,is_default,is_active)
                VALUES(?,?,'Test Size',?,TRUE,TRUE)
                """, productId, sku, price);
        long variantId = jdbc.queryForObject(
                "SELECT id FROM product_variants WHERE sku=?", Long.class, sku);
        jdbc.update("""
                INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level)
                VALUES(?,?,0,0)
                """, variantId, stock);
        return variantId;
    }

    private long createAddress(long userId) {
        jdbc.update("""
                INSERT INTO addresses(user_id,recipient_name,phone,address_line1,city,country_code)
                VALUES(?, 'Payment Recipient','01700000000','1 Payment Test Road','Dhaka','BD')
                """, userId);
        return jdbc.queryForObject(
                "SELECT id FROM addresses WHERE user_id=? ORDER BY id DESC LIMIT 1", Long.class, userId);
    }

    private record Session(Cookie access, long userId) {}
}

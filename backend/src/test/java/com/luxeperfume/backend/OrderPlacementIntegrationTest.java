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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OrderPlacementIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void createsOrderSnapshotsReservesStockRedeemsCouponAndConvertsCartAtomically() throws Exception {
        Session customer = register("order-create");
        long variantId = createVariant(new BigDecimal("1000.00"), 6);
        addToCart(customer.userId(), variantId, 2);
        long addressId = createAddress(customer.userId());
        String couponCode = createCoupon("FIXED", new BigDecimal("100.00"), 1, 1);

        MvcResult result = mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("""
                        {"addressId": %d, "couponCode": "%s", "customerNote": "Please call before delivery"}
                        """.formatted(addressId, couponCode)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.orderStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.paymentStatus").value("UNPAID"))
            .andExpect(jsonPath("$.data.subtotal").value(2000))
            .andExpect(jsonPath("$.data.discountTotal").value(100))
            .andExpect(jsonPath("$.data.grandTotal").value(1900))
            .andExpect(jsonPath("$.data.items[0].unitPrice").value(1000))
            .andExpect(jsonPath("$.data.items[0].quantity").value(2))
            .andReturn();

        String body = result.getResponse().getContentAsString();
        String orderNumber = com.jayway.jsonpath.JsonPath.read(body, "$.data.orderNumber");
        org.assertj.core.api.Assertions.assertThat(orderNumber).matches("LP-[0-9]{8}-[A-F0-9]{12}");

        long orderId = jdbc.queryForObject("SELECT id FROM customer_orders WHERE order_number=?",
                Long.class, orderNumber);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_items WHERE order_id=? AND product_name_snapshot='Order Test Product'",
                Integer.class, orderId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT customer_note FROM customer_orders WHERE id=?", String.class, orderId))
                .isEqualTo("Please call before delivery");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isEqualTo(2);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RESERVATION' AND quantity_change=2
                  AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT usage_count FROM coupons WHERE code=?", Integer.class, couponCode)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id=(SELECT id FROM coupons WHERE code=?) AND order_id=?",
                Integer.class, couponCode, orderId)).isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT cart_status FROM carts WHERE user_id=? AND cart_status='CONVERTED'",
                String.class, customer.userId())).isEqualTo("CONVERTED");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id=? AND from_status IS NULL AND to_status='PENDING'",
                Integer.class, orderId)).isEqualTo(1);
    }

    @Test
    void rejectsInsufficientStockWithoutPersistingOrderOrConvertingCart() throws Exception {
        Session customer = register("order-stock-race");
        long variantId = createVariant(new BigDecimal("500.00"), 1);
        addToCart(customer.userId(), variantId, 1);
        long addressId = createAddress(customer.userId());
        jdbc.update("UPDATE inventory SET quantity_reserved=1 WHERE variant_id=?", variantId);

        mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isConflict());

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE user_id=?", Integer.class, customer.userId()))
                .isZero();
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT cart_status FROM carts WHERE user_id=? AND cart_status='ACTIVE'",
                String.class, customer.userId())).isEqualTo("ACTIVE");
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE variant_id=? AND movement_type='RESERVATION'",
                Integer.class, variantId)).isZero();
    }

    @Test
    void secondSubmissionCannotCreateAnotherOrderFromConvertedCart() throws Exception {
        Session customer = register("order-double-submit");
        long variantId = createVariant(new BigDecimal("650.00"), 3);
        addToCart(customer.userId(), variantId, 1);
        long addressId = createAddress(customer.userId());
        String request = "{\"addressId\":" + addressId + "}";

        mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json").content(request))
            .andExpect(status().isOk());
        mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json").content(request))
            .andExpect(status().isConflict());

        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE user_id=?", Integer.class, customer.userId()))
                .isEqualTo(1);
        org.assertj.core.api.Assertions.assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isEqualTo(1);
    }

    private Session register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Phase9\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        return new Session(access, userId);
    }

    private long createVariant(BigDecimal price, int stock) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE slug='floral'", Long.class);
        String slug = "phase9-order-" + UUID.randomUUID();
        jdbc.update("INSERT INTO products(category_id,name,slug,product_type,status) VALUES(?,?,'"+slug+"','VARIABLE','ACTIVE')",
                categoryId, "Order Test Product");
        long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug=?", Long.class, slug);
        String sku = "ORD-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_variants(product_id,sku,variant_name,price,is_default,is_active)
                VALUES(?,?,'Test Size',?,TRUE,TRUE)
                """, productId, sku, price);
        long variantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?", Long.class, sku);
        jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,?,0,0)",
                variantId, stock);
        return variantId;
    }

    private void addToCart(long userId, long variantId, int quantity) {
        jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", userId);
        long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'",
                Long.class, userId);
        jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)",
                cartId, variantId, quantity);
    }

    private long createAddress(long userId) {
        jdbc.update("""
                INSERT INTO addresses(user_id,recipient_name,phone,address_line1,city,country_code)
                VALUES(?, 'Order Recipient','01700000000','1 Order Test Road','Dhaka','BD')
                """, userId);
        return jdbc.queryForObject("SELECT id FROM addresses WHERE user_id=? ORDER BY id DESC LIMIT 1",
                Long.class, userId);
    }

    private String createCoupon(String type, BigDecimal value, int usageLimit, int perUserLimit) {
        String code = "ORD-" + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO coupons(code,discount_type,discount_value,minimum_order_amount,
                                    usage_limit,per_user_limit,usage_count,is_active)
                VALUES(?,?,?,0,?,?,0,TRUE)
                """, code, type, value, usageLimit, perUserLimit);
        return code;
    }

    private record Session(Cookie access, long userId) {}
}

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CustomerOrderManagementIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void customerCanSeeOnlyOwnHistoryDetailsAndTracking() throws Exception {
        Session owner = register("order-owner");
        Session other = register("order-other");
        String orderNumber = placeOrder(owner, 1);

        mockMvc.perform(get("/api/v1/orders").cookie(owner.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].orderNumber").value(orderNumber));

        mockMvc.perform(get("/api/v1/orders").cookie(other.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(0));

        mockMvc.perform(get("/api/v1/orders/" + orderNumber).cookie(owner.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderNumber").value(orderNumber))
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.statusHistory[0].toStatus").value("PENDING"));

        mockMvc.perform(get("/api/v1/orders/" + orderNumber).cookie(other.access()))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/orders/" + orderNumber + "/tracking").cookie(owner.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.currentStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.events.length()").value(1));
    }

    @Test
    void cancellationReleasesReservedStockExactlyOnceAndWritesAuditHistory() throws Exception {
        Session customer = register("order-cancel");
        String orderNumber = placeOrder(customer, 2);
        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        long variantId = jdbc.queryForObject(
                "SELECT variant_id FROM order_items WHERE order_id=?", Long.class, orderId);

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isEqualTo(2);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel")
                .with(csrf()).cookie(customer.access()).contentType("application/json")
                .content("{\"reason\":\"Customer changed delivery plans\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.data.releasedInventory[0].quantityReleased").value(2));

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND quantity_change=-2
                  AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM order_status_history
                WHERE order_id=? AND from_status='PENDING' AND toStatus='CANCELLED'
                """, Integer.class, orderId)).isEqualTo(1);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel")
                .with(csrf()).cookie(customer.access()).contentType("application/json")
                .content("{}"))
            .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isEqualTo(1);
    }

    @Test
    void shippedOrderCannotBeCustomerCancelled() throws Exception {
        Session customer = register("order-shipped");
        String orderNumber = placeOrder(customer, 1);
        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        long variantId = jdbc.queryForObject(
                "SELECT variant_id FROM order_items WHERE order_id=?", Long.class, orderId);
        jdbc.update("UPDATE customer_orders SET order_status='SHIPPED' WHERE id=?", orderId);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel")
                .with(csrf()).cookie(customer.access()).contentType("application/json")
                .content("{}"))
            .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isZero();
    }

    @Test
    void paidOrderCannotBeCustomerCancelledBeforeRefundWorkflowExists() throws Exception {
        Session customer = register("order-paid");
        String orderNumber = placeOrder(customer, 1);
        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        long variantId = jdbc.queryForObject(
                "SELECT variant_id FROM order_items WHERE order_id=?", Long.class, orderId);
        jdbc.update("UPDATE customer_orders SET payment_status='PAID' WHERE id=?", orderId);

        mockMvc.perform(post("/api/v1/orders/" + orderNumber + "/cancel")
                .with(csrf()).cookie(customer.access()).contentType("application/json")
                .content("{}"))
            .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isEqualTo(1);
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isZero();
    }

    private String placeOrder(Session customer, int quantity) throws Exception {
        long variantId = createVariant(new BigDecimal("725.00"), 8);
        addToCart(customer.userId(), variantId, quantity);
        long addressId = createAddress(customer.userId());
        MvcResult result = mockMvc.perform(post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isOk())
            .andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(),
                "$.data.orderNumber");
    }

    private Session register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email
                        + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Phase9\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        return new Session(access, userId);
    }

    private long createVariant(BigDecimal price, int stock) {
        long categoryId = jdbc.queryForObject(
                "SELECT id FROM categories WHERE slug='floral'", Long.class);
        String slug = "phase9-customer-order-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO products(category_id,name,slug,product_type,status)
                VALUES(?,? ,?,'VARIABLE','ACTIVE')
                """, categoryId, "Customer Order Test Product", slug);
        long productId = jdbc.queryForObject(
                "SELECT id FROM products WHERE slug=?", Long.class, slug);
        String sku = "CUST-ORD-" + UUID.randomUUID();
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

    private void addToCart(long userId, long variantId, int quantity) {
        jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", userId);
        long cartId = jdbc.queryForObject(
                "SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'", Long.class, userId);
        jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)",
                cartId, variantId, quantity);
    }

    private long createAddress(long userId) {
        jdbc.update("""
                INSERT INTO addresses(user_id,recipient_name,phone,address_line1,city,country_code)
                VALUES(?, 'Order Recipient','01700000000','1 Order Test Road','Dhaka','BD')
                """, userId);
        return jdbc.queryForObject(
                "SELECT id FROM addresses WHERE user_id=? ORDER BY id DESC LIMIT 1", Long.class, userId);
    }

    private record Session(Cookie access, long userId) {}
}
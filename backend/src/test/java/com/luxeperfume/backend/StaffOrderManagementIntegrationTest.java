package com.luxeperfume.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class StaffOrderManagementIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    @Transactional
    void managerCanSearchInspectAndAdvanceOrderWithAuditHistory() throws Exception {
        Session manager = register("staff-order-flow");
        addRole(manager.userId(), "MANAGER");
        String orderNumber = placeOrder(manager, 2);

        mockMvc.perform(get("/api/v1/staff/orders")
                .cookie(manager.access()).param("status", "PENDING").param("q", orderNumber)
                .param("page", "0").param("size", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totalElements").value(1))
            .andExpect(jsonPath("$.data.items[0].orderNumber").value(orderNumber))
            .andExpect(jsonPath("$.data.items[0].itemCount").value(2));

        mockMvc.perform(get("/api/v1/staff/orders/" + orderNumber).cookie(manager.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.statusHistory[0].toStatus").value("PENDING"));

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"CONFIRMED\",\"note\":\"Payment on delivery confirmed\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.previousStatus").value("PENDING"))
            .andExpect(jsonPath("$.data.orderStatus").value("CONFIRMED"));

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"PROCESSING\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("PROCESSING"));

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"SHIPPED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("SHIPPED"));

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"DELIVERED\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("DELIVERED"));

        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM order_status_history WHERE order_id=?", Integer.class, orderId))
                .isEqualTo(5);
        assertThat(jdbc.queryForObject(
                "SELECT changed_by FROM order_status_history WHERE order_id=? AND to_status='DELIVERED'",
                Long.class, orderId)).isEqualTo(manager.userId());

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"PROCESSING\"}"))
            .andExpect(status().isConflict());
    }

    @Test
    @Transactional
    void staffCancellationReleasesInventoryExactlyOnce() throws Exception {
        Session manager = register("staff-order-cancel");
        addRole(manager.userId(), "MANAGER");
        String orderNumber = placeOrder(manager, 3);
        long orderId = jdbc.queryForObject(
                "SELECT id FROM customer_orders WHERE order_number=?", Long.class, orderNumber);
        long variantId = jdbc.queryForObject(
                "SELECT variant_id FROM order_items WHERE order_id=?", Long.class, orderId);

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"CANCELLED\",\"note\":\"Customer support approved cancellation\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.orderStatus").value("CANCELLED"))
            .andExpect(jsonPath("$.data.releasedInventory[0].quantityReleased").value(3));

        assertThat(jdbc.queryForObject(
                "SELECT quantity_reserved FROM inventory WHERE variant_id=?", Integer.class, variantId))
                .isZero();
        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isEqualTo(1);

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(manager.access()).contentType("application/json")
                .content("{\"status\":\"CANCELLED\"}"))
            .andExpect(status().isConflict());

        assertThat(jdbc.queryForObject("""
                SELECT COUNT(*) FROM inventory_movements
                WHERE variant_id=? AND movement_type='RELEASE' AND reference_type='ORDER' AND reference_id=?
                """, Integer.class, variantId, orderId)).isEqualTo(1);
    }

    @Test
    @Transactional
    void orderReadPermissionDoesNotGrantStatusWritePermission() throws Exception {
        Session staff = register("staff-order-readonly");
        long roleId = roleId("MANAGER");
        long permissionId = jdbc.queryForObject(
                "SELECT id FROM permissions WHERE code='orders.write'", Long.class);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", staff.userId(), roleId);
        jdbc.update("DELETE FROM role_permissions WHERE role_id=? AND permission_id=?", roleId, permissionId);
        String orderNumber = placeOrder(staff, 1);

        mockMvc.perform(get("/api/v1/staff/orders").cookie(staff.access()))
            .andExpect(status().isOk());

        mockMvc.perform(patch("/api/v1/staff/orders/" + orderNumber + "/status")
                .with(csrf()).cookie(staff.access()).contentType("application/json")
                .content("{\"status\":\"CONFIRMED\"}"))
            .andExpect(status().isForbidden());
    }

    private String placeOrder(Session customer, int quantity) throws Exception {
        long variantId = createVariant(new BigDecimal("725.00"), 10);
        jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", customer.userId());
        long cartId = jdbc.queryForObject(
                "SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'", Long.class, customer.userId());
        jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)", cartId, variantId, quantity);
        jdbc.update("""
                INSERT INTO addresses(user_id,recipient_name,phone,address_line1,city,country_code)
                VALUES(?, 'Staff Order Recipient','01700000000','1 Staff Test Road','Dhaka','BD')
                """, customer.userId());
        long addressId = jdbc.queryForObject(
                "SELECT id FROM addresses WHERE user_id=? ORDER BY id DESC LIMIT 1", Long.class, customer.userId());
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/orders").with(csrf()).cookie(customer.access())
                .contentType("application/json").content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isOk()).andReturn();
        return com.jayway.jsonpath.JsonPath.read(result.getResponse().getContentAsString(), "$.data.orderNumber");
    }

    private long createVariant(BigDecimal price, int stock) {
        long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE slug='floral'", Long.class);
        String slug = "phase9-staff-order-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO products(category_id,name,slug,product_type,status)
                VALUES(?,? ,?,'VARIABLE','ACTIVE')
                """, categoryId, "Staff Order Test Product", slug);
        long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug=?", Long.class, slug);
        String sku = "STAFF-ORD-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO product_variants(product_id,sku,variant_name,price,is_default,is_active)
                VALUES(?,?,'Test Size',?,TRUE,TRUE)
                """, productId, sku, price);
        long variantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?", Long.class, sku);
        jdbc.update("""
                INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level)
                VALUES(?,?,0,0)
                """, variantId, stock);
        return variantId;
    }

    private Session register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .post("/api/v1/auth/register").with(csrf()).contentType("application/json")
                .content("{\"email\":\"" + email
                        + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Phase9Staff\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        return new Session(access, userId);
    }

    private void addRole(long userId, String code) {
        jdbc.update("INSERT INTO user_roles(user_id,role_id) VALUES(?,?)", userId, roleId(code));
    }

    private long roleId(String code) {
        return jdbc.queryForObject("SELECT id FROM roles WHERE code=?", Long.class, code);
    }

    private record Session(Cookie access, long userId) {}
}

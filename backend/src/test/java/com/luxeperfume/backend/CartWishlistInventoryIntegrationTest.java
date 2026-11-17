package com.luxeperfume.backend;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CartWishlistInventoryIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void cartPersistsUpdatesQuantityRemovesAndAlwaysUsesCurrentDatabasePrice() throws Exception {
        Session customer = register("cart");
        long variantId = createActiveVariant(new BigDecimal("1250.00"), 8, 2);

        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"variantId\":" + variantId + ",\"quantity\":2,\"unitPrice\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.items[0].quantity").value(2))
            .andExpect(jsonPath("$.data.subtotal").value(2500));

        mockMvc.perform(get("/api/v1/cart").cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1))
            .andExpect(jsonPath("$.data.subtotal").value(2500));

        jdbc.update("UPDATE product_variants SET price=1400 WHERE id=?", variantId);
        mockMvc.perform(get("/api/v1/cart").cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].currentUnitPrice").value(1400))
            .andExpect(jsonPath("$.data.subtotal").value(2800));

        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"variantId\":" + variantId + ",\"quantity\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].quantity").value(3))
            .andExpect(jsonPath("$.data.subtotal").value(4200));

        long itemId = jdbc.queryForObject(
                "SELECT ci.id FROM cart_items ci JOIN carts c ON c.id=ci.cart_id WHERE c.user_id=? AND ci.variant_id=?",
                Long.class, customer.userId(), variantId);
        mockMvc.perform(put("/api/v1/cart/items/" + itemId).with(csrf()).cookie(customer.access())
                .contentType("application/json").content("{\"quantity\":4}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items[0].quantity").value(4))
            .andExpect(jsonPath("$.data.subtotal").value(5600));

        mockMvc.perform(delete("/api/v1/cart/items/" + itemId).with(csrf()).cookie(customer.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    @Test
    void cartRejectsQuantityAboveAvailableStockAndIsPrivatePerCustomer() throws Exception {
        Session first = register("cart-owner");
        Session second = register("cart-other");
        long variantId = createActiveVariant(new BigDecimal("900.00"), 2, 1);

        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).cookie(first.access())
                .contentType("application/json")
                .content("{\"variantId\":" + variantId + ",\"quantity\":3}"))
            .andExpect(status().isConflict());

        mockMvc.perform(post("/api/v1/cart/items").with(csrf()).cookie(first.access())
                .contentType("application/json")
                .content("{\"variantId\":" + variantId + ",\"quantity\":2}"))
            .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/cart").cookie(second.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(get("/api/v1/cart"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void wishlistPersistsIsIdempotentAndIsolatedBetweenCustomers() throws Exception {
        Session first = register("wishlist-owner");
        Session second = register("wishlist-other");
        long productId = createActiveProduct();

        mockMvc.perform(post("/api/v1/wishlist/items/" + productId).with(csrf()).cookie(first.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1));
        mockMvc.perform(post("/api/v1/wishlist/items/" + productId).with(csrf()).cookie(first.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.items.length()").value(1));
        mockMvc.perform(get("/api/v1/wishlist").cookie(first.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.itemCount").value(1));
        mockMvc.perform(get("/api/v1/wishlist").cookie(second.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.itemCount").value(0));
        mockMvc.perform(delete("/api/v1/wishlist/items/" + productId).with(csrf()).cookie(first.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.itemCount").value(0));
        mockMvc.perform(get("/api/v1/wishlist"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void inventoryAdjustmentIsAuditedAndCannotReduceBelowReservedStock() throws Exception {
        Session staff = register("inventory-staff");
        grantInventoryStaff(staff.userId());
        long variantId = createActiveVariant(new BigDecimal("1100.00"), 10, 3);

        mockMvc.perform(post("/api/v1/staff/inventory/variants/" + variantId + "/adjustments")
                .with(csrf()).cookie(staff.access())
                .contentType("application/json").content("{\"quantityChange\":-6,\"note\":\"Cycle count correction\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity_on_hand").value(4))
            .andExpect(jsonPath("$.data.available_quantity").value(4));

        assertThat(jdbc.queryForObject(
                "SELECT COUNT(*) FROM inventory_movements WHERE variant_id=? AND movement_type='ADJUSTMENT' AND quantity_change=-6",
                Integer.class, variantId)).isEqualTo(1);

        jdbc.update("UPDATE inventory SET quantity_reserved=3 WHERE variant_id=?", variantId);
        mockMvc.perform(post("/api/v1/staff/inventory/variants/" + variantId + "/adjustments")
                .with(csrf()).cookie(staff.access())
                .contentType("application/json").content("{\"quantityChange\":-2,\"note\":\"Must be rejected\"}"))
            .andExpect(status().isConflict());

        mockMvc.perform(get("/api/v1/staff/inventory/variants/" + variantId).cookie(staff.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.quantity_on_hand").value(4))
            .andExpect(jsonPath("$.data.available_quantity").value(1));

        mockMvc.perform(get("/api/v1/staff/inventory/low-stock").cookie(staff.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].variant_id").value(variantId));

        mockMvc.perform(get("/api/v1/staff/inventory/variants/" + variantId + "/movements").cookie(staff.access()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data[0].movement_type").value("ADJUSTMENT"));
    }

    @Test
    void customerCannotReadOrAdjustStaffInventory() throws Exception {
        Session customer = register("inventory-customer");
        long variantId = createActiveVariant(new BigDecimal("700.00"), 5, 1);
        mockMvc.perform(get("/api/v1/staff/inventory/low-stock").cookie(customer.access()))
            .andExpect(status().isForbidden());
        mockMvc.perform(post("/api/v1/staff/inventory/variants/" + variantId + "/adjustments")
                .with(csrf()).cookie(customer.access())
                .contentType("application/json").content("{\"quantityChange\":1,\"note\":\"Not allowed\"}"))
            .andExpect(status().isForbidden());
    }

    private Session register(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@example.invalid";
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register").with(csrf())
                .contentType("application/json")
                .content("{\"email\":\"" + email + "\",\"password\":\"StrongPassphrase123!\",\"firstName\":\"Phase8\"}"))
            .andExpect(status().isOk()).andReturn();
        Cookie access = result.getResponse().getCookie("lp_access");
        Long userId = jdbc.queryForObject("SELECT id FROM users WHERE email=?", Long.class, email);
        return new Session(access, userId);
    }

    private long createActiveProduct() {
        Long categoryId = jdbc.queryForObject("SELECT id FROM categories WHERE slug='floral'", Long.class);
        String slug = "phase8-product-" + UUID.randomUUID();
        jdbc.update("INSERT INTO products(category_id,name,slug,product_type,status) VALUES(?,?,?,'VARIABLE','ACTIVE')",
                categoryId, "Phase 8 Test Product", slug);
        return jdbc.queryForObject("SELECT id FROM products WHERE slug=?", Long.class, slug);
    }

    private long createActiveVariant(BigDecimal price, int stock, int reorderLevel) {
        long productId = createActiveProduct();
        String sku = "P8-" + UUID.randomUUID();
        jdbc.update("INSERT INTO product_variants(product_id,sku,variant_name,price,is_default,is_active) VALUES(?,?, 'Test size', ?,TRUE,TRUE)",
                productId, sku, price);
        long variantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?", Long.class, sku);
        jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,?,0,?)",
                variantId, stock, reorderLevel);
        return variantId;
    }

    private void grantInventoryStaff(long userId) {
        jdbc.update("DELETE FROM user_roles WHERE user_id=?", userId);
        jdbc.update("INSERT INTO user_roles(user_id,role_id) SELECT ?,id FROM roles WHERE code='INVENTORY_STAFF'", userId);
    }

    private record Session(Cookie access, long userId) {}
}

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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutPreviewIntegrationTest {
    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbc;

    @Test
    void previewUsesDatabasePricesAndCalculatesPercentageCouponOnServer() throws Exception {
        Session customer = register("checkout-percent");
        long variantId = createVariant(new BigDecimal("1200.00"), 8);
        addToCart(customer.userId(), variantId, 2);
        long addressId = createAddress(customer.userId());
        long couponId = createCoupon("PERCENTAGE", new BigDecimal("10.00"),
                new BigDecimal("150.00"), BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + ",\"couponCode\":\"PCT-" + couponId
                        + "\",\"subtotal\":1,\"discountTotal\":99999,\"shippingTotal\":99999,\"grandTotal\":1}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totals.subtotal").value(2400))
            .andExpect(jsonPath("$.data.totals.discountTotal").value(150))
            .andExpect(jsonPath("$.data.totals.shippingTotal").value(0))
            .andExpect(jsonPath("$.data.totals.taxTotal").value(0))
            .andExpect(jsonPath("$.data.totals.grandTotal").value(2250))
            .andExpect(jsonPath("$.data.items[0].unitPrice").value(1200))
            .andExpect(jsonPath("$.data.items[0].lineDiscount").value(150))
            .andExpect(jsonPath("$.data.stockAvailable").value(true));
    }

    @Test
    void previewCalculatesFixedCouponAndDoesNotTrustClientPrice() throws Exception {
        Session customer = register("checkout-fixed");
        long variantId = createVariant(new BigDecimal("700.00"), 5);
        addToCart(customer.userId(), variantId, 2);
        long addressId = createAddress(customer.userId());
        long couponId = createCoupon("FIXED", new BigDecimal("250.00"), null, BigDecimal.ZERO);

        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + ",\"couponCode\":\"FIX-" + couponId + "\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data.totals.subtotal").value(1400))
            .andExpect(jsonPath("$.data.totals.discountTotal").value(250))
            .andExpect(jsonPath("$.data.totals.grandTotal").value(1150));
    }

    @Test
    void previewRejectsAddressOwnedByAnotherCustomerAndInsufficientStock() throws Exception {
        Session owner = register("checkout-address-owner");
        Session other = register("checkout-address-other");
        long addressId = createAddress(owner.userId());

        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf()).cookie(other.access())
                .contentType("application/json").content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isNotFound());

        long variantId = createVariant(new BigDecimal("500.00"), 1);
        addToCart(other.userId(), variantId, 2);
        long ownAddress = createAddress(other.userId());
        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf()).cookie(other.access())
                .contentType("application/json").content("{\"addressId\":" + ownAddress + "}"))
            .andExpect(status().isConflict());
    }

    @Test
    void previewRejectsCouponBelowMinimumAndUnauthenticatedAccess() throws Exception {
        Session customer = register("checkout-minimum");
        long variantId = createVariant(new BigDecimal("500.00"), 5);
        addToCart(customer.userId(), variantId, 2);
        long addressId = createAddress(customer.userId());
        long couponId = createCoupon("FIXED", new BigDecimal("50.00"), null, new BigDecimal("1500.00"));

        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf()).cookie(customer.access())
                .contentType("application/json")
                .content("{\"addressId\":" + addressId + ",\"couponCode\":\"FIX-" + couponId + "\"}"))
            .andExpect(status().isUnprocessableEntity());

        mockMvc.perform(post("/api/v1/checkout/preview").with(csrf())
                .contentType("application/json").content("{\"addressId\":" + addressId + "}"))
            .andExpect(status().isUnauthorized());
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
        String slug = "phase9-product-" + UUID.randomUUID();
        jdbc.update("INSERT INTO products(category_id,name,slug,product_type,status) VALUES(?,?,?,'VARIABLE','ACTIVE')",
                categoryId, "Phase 9 Checkout Test", slug);
        long productId = jdbc.queryForObject("SELECT id FROM products WHERE slug=?", Long.class, slug);
        String sku = "P9-" + UUID.randomUUID();
        jdbc.update("INSERT INTO product_variants(product_id,sku,variant_name,price,is_default,is_active) VALUES(?,?, 'Test size', ?,TRUE,TRUE)",
                productId, sku, price);
        long variantId = jdbc.queryForObject("SELECT id FROM product_variants WHERE sku=?", Long.class, sku);
        jdbc.update("INSERT INTO inventory(variant_id,quantity_on_hand,quantity_reserved,reorder_level) VALUES(?,?,0,0)",
                variantId, stock);
        return variantId;
    }

    private void addToCart(long userId, long variantId, int quantity) {
        jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", userId);
        long cartId = jdbc.queryForObject("SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'", Long.class, userId);
        jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)", cartId, variantId, quantity);
    }

    private long createAddress(long userId) {
        jdbc.update("INSERT INTO addresses(user_id,recipient_name,phone,address_line1,city,country_code) VALUES(?, 'Test Recipient','01700000000','1 Test Road','Dhaka','BD')", userId);
        return jdbc.queryForObject("SELECT id FROM addresses WHERE user_id=? ORDER BY id DESC LIMIT 1", Long.class, userId);
    }

    private long createCoupon(String type, BigDecimal value, BigDecimal cap, BigDecimal minimum) {
        String code = ("PERCENTAGE".equals(type) ? "PCT-" : "FIX-") + UUID.randomUUID().toString().substring(0, 8);
        jdbc.update("""
                INSERT INTO coupons(code,discount_type,discount_value,minimum_order_amount,maximum_discount_amount,
                                    usage_limit,per_user_limit,usage_count,is_active)
                VALUES(?,?,?,?,?,NULL,NULL,0,TRUE)
                """, code, type, value, minimum, cap);
        return jdbc.queryForObject("SELECT id FROM coupons WHERE code=?", Long.class, code);
    }

    private record Session(Cookie access, long userId) {}
}

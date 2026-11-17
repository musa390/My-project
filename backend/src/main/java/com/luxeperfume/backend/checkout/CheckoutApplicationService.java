package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;
import com.luxeperfume.backend.checkout.domain.Coupon;
import com.luxeperfume.backend.checkout.dto.CheckoutPreviewRequest;
import com.luxeperfume.backend.checkout.dto.CheckoutQuoteResponse;
import com.luxeperfume.backend.checkout.pricing.CheckoutQuoteService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class CheckoutApplicationService {
    private final JdbcTemplate jdbc;
    private final CheckoutQuoteService quoteService;

    public CheckoutApplicationService(JdbcTemplate jdbc, CheckoutQuoteService quoteService) {
        this.jdbc = jdbc;
        this.quoteService = quoteService;
    }

    @Transactional(readOnly = true)
    public CheckoutQuoteResponse preview(long userId, CheckoutPreviewRequest request) {
        Map<String, Object> address = jdbc.queryForList("""
                SELECT id,recipient_name,phone,address_line1,address_line2,area,city,district,postal_code,country_code
                FROM addresses WHERE id=? AND user_id=?
                """, request.addressId(), userId).stream().findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery address not found"));

        List<Map<String, Object>> cartRows = jdbc.queryForList("""
                SELECT pv.id AS variant_id,pv.product_id,p.category_id,p.name AS product_name,
                       pv.variant_name,pv.sku,pv.price,ci.quantity,
                       GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) AS available_stock,
                       p.status AS product_status,p.deleted_at,pv.is_active AS variant_active
                FROM carts c
                JOIN cart_items ci ON ci.cart_id=c.id
                JOIN product_variants pv ON pv.id=ci.variant_id
                JOIN products p ON p.id=pv.product_id
                LEFT JOIN inventory i ON i.variant_id=pv.id
                WHERE c.user_id=? AND c.cart_status='ACTIVE'
                ORDER BY pv.id
                """, userId);
        if (cartRows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty");

        List<CheckoutLine> lines = cartRows.stream().map(row -> {
            String status = (String) row.get("product_status");
            Object deletedAt = row.get("deleted_at");
            Object active = row.get("variant_active");
            long available = ((Number) row.get("available_stock")).longValue();
            int quantity = ((Number) row.get("quantity")).intValue();
            if (!"ACTIVE".equals(status) || deletedAt != null || !truthy(active))
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart product is no longer available");
            if (available < quantity)
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart item has insufficient available stock");
            return new CheckoutLine(
                    ((Number) row.get("product_id")).longValue(),
                    ((Number) row.get("category_id")).longValue(),
                    ((Number) row.get("variant_id")).longValue(),
                    (String) row.get("product_name"), (String) row.get("variant_name"),
                    (String) row.get("sku"), quantity, (BigDecimal) row.get("price"), available);
        }).toList();

        Coupon coupon = request.couponCode() == null || request.couponCode().isBlank()
                ? null : loadAndValidateCoupon(request.couponCode().trim(), userId, lines);
        CheckoutQuoteService.Quote quote;
        try {
            quote = quoteService.calculate(lines, coupon, (String) address.get("country_code"),
                    (String) address.get("city"), (String) address.get("area"));
        } catch (IllegalArgumentException invalidCoupon) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, invalidCoupon.getMessage());
        }

        String email = jdbc.queryForObject("SELECT email FROM users WHERE id=? AND account_status='ACTIVE' AND deleted_at IS NULL",
                String.class, userId);
        boolean stockAvailable = lines.stream().allMatch(line -> line.availableStock() >= line.quantity());
        return new CheckoutQuoteResponse(
                ((Number) address.get("id")).longValue(),
                (String) address.get("recipient_name"), (String) address.get("phone"),
                (String) address.get("address_line1"), (String) address.get("address_line2"),
                (String) address.get("area"), (String) address.get("city"),
                (String) address.get("district"), (String) address.get("postal_code"),
                (String) address.get("country_code"), email, "BDT", quote.couponCode(),
                CheckoutQuoteResponse.Money.from(quote.totals()), quote.lines(), stockAvailable,
                "Preview only. Prices, coupon limits and stock will be revalidated when the order is placed.");
    }

    private Coupon loadAndValidateCoupon(String rawCode, long userId, List<CheckoutLine> lines) {
        List<Map<String, Object>> rows = jdbc.queryForList("""
                SELECT id,code,discount_type,discount_value,minimum_order_amount,maximum_discount_amount,
                       usage_limit,per_user_limit,usage_count,starts_at,ends_at,is_active
                FROM coupons WHERE code=?
                """, rawCode);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is invalid or unavailable");
        Map<String, Object> row = rows.getFirst();
        if (!truthy(row.get("is_active"))) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is inactive");
        java.time.LocalDateTime now = java.time.LocalDateTime.now(java.time.Clock.systemUTC());
        java.time.LocalDateTime starts = dateTime(row.get("starts_at"));
        java.time.LocalDateTime ends = dateTime(row.get("ends_at"));
        if (starts != null && now.isBefore(starts)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is not active yet");
        if (ends != null && now.isAfter(ends)) throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon has expired");
        Object usageLimit = row.get("usage_limit");
        if (usageLimit != null && ((Number) row.get("usage_count")).longValue() >= ((Number) usageLimit).longValue())
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon usage limit has been reached");
        Object perUserLimit = row.get("per_user_limit");
        if (perUserLimit != null) {
            Integer used = jdbc.queryForObject("SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id=? AND user_id=?",
                    Integer.class, row.get("id"), userId);
            if (used != null && used >= ((Number) perUserLimit).intValue())
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "You have reached this coupon's usage limit");
        }

        long couponId = ((Number) row.get("id")).longValue();
        Set<Long> productIds = new HashSet<>(jdbc.query(
                "SELECT product_id FROM coupon_products WHERE coupon_id=?",
                (rs, n) -> rs.getLong(1), couponId));
        Set<Long> categoryIds = new HashSet<>(jdbc.query(
                "SELECT category_id FROM coupon_categories WHERE coupon_id=?",
                (rs, n) -> rs.getLong(1), couponId));
        boolean restricted = !productIds.isEmpty() || !categoryIds.isEmpty();
        BigDecimal subtotal = lines.stream().map(CheckoutLine::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo((BigDecimal) row.get("minimum_order_amount")) < 0)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Cart does not meet the coupon minimum order amount");
        BigDecimal eligibleSubtotal = lines.stream()
                .filter(line -> !restricted || productIds.contains(line.productId()) || categoryIds.contains(line.categoryId()))
                .map(CheckoutLine::lineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (eligibleSubtotal.signum() <= 0)
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon does not apply to items in your cart");

        return new Coupon((String) row.get("code"), (String) row.get("discount_type"),
                (BigDecimal) row.get("discount_value"), (BigDecimal) row.get("minimum_order_amount"),
                (BigDecimal) row.get("maximum_discount_amount"), eligibleSubtotal,
                productIds, categoryIds, restricted);
    }

    private static java.time.LocalDateTime dateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof java.time.LocalDateTime dateTime) return dateTime;
        throw new IllegalStateException("Unexpected coupon date type");
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        return value instanceof Number number && number.intValue() != 0;
    }
}

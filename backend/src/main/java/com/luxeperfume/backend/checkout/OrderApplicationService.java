package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.domain.CheckoutLine;
import com.luxeperfume.backend.checkout.domain.Coupon;
import com.luxeperfume.backend.checkout.domain.OrderNumberGenerator;
import com.luxeperfume.backend.inventory.domain.InventoryQuantity;
import com.luxeperfume.backend.notification.NotificationService;
import com.luxeperfume.backend.checkout.dto.OrderPlacementRequest;
import com.luxeperfume.backend.checkout.dto.OrderPlacementResponse;
import com.luxeperfume.backend.checkout.inventory.InventoryReservationService;
import com.luxeperfume.backend.checkout.pricing.CheckoutQuoteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
/** [ENC] Order placement consumes validated domain values and persists immutable item snapshots transactionally. */
public class OrderApplicationService {
    private final JdbcTemplate jdbc;
    private final CheckoutQuoteService quoteService;
    private final InventoryReservationService inventoryReservationService;
    private final OrderNumberGenerator orderNumberGenerator;
    private final NotificationService notificationService;
    private final Clock clock;

    @Autowired
    public OrderApplicationService(JdbcTemplate jdbc, CheckoutQuoteService quoteService,
                                   InventoryReservationService inventoryReservationService,
                                   OrderNumberGenerator orderNumberGenerator,
                                   NotificationService notificationService) {
        this(jdbc, quoteService, inventoryReservationService, orderNumberGenerator,
                notificationService, Clock.systemUTC());
    }

    OrderApplicationService(JdbcTemplate jdbc, CheckoutQuoteService quoteService,
                            InventoryReservationService inventoryReservationService,
                            OrderNumberGenerator orderNumberGenerator,
                            NotificationService notificationService, Clock clock) {
        this.jdbc = jdbc;
        this.quoteService = quoteService;
        this.inventoryReservationService = inventoryReservationService;
        this.orderNumberGenerator = orderNumberGenerator;
        this.notificationService = notificationService;
        this.clock = clock;
    }

    /**
     * One transaction owns the active cart, current prices, inventory reservations,
     * order snapshots and coupon redemption. Any failure rolls every write back.
     */
    @Transactional(rollbackFor = Exception.class)
    public OrderPlacementResponse placeOrder(long userId, OrderPlacementRequest request) {
        Map<String, Object> cart = oneOrNull("""
                SELECT id,currency FROM carts
                WHERE user_id=? AND cart_status='ACTIVE'
                FOR UPDATE
                """, userId);
        if (cart == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "No active cart is available. The cart may already have been checked out.");
        }
        long cartId = number(cart.get("id"));
        String currency = (String) cart.get("currency");

        Map<String, Object> address = oneOrNull("""
                SELECT id,recipient_name,phone,address_line1,address_line2,area,city,district,
                       postal_code,country_code
                FROM addresses
                WHERE id=? AND user_id=?
                FOR UPDATE
                """, request.addressId(), userId);
        if (address == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Delivery address not found");
        }

        String customerEmail = jdbc.query("""
                SELECT email FROM users
                WHERE id=? AND account_status='ACTIVE' AND deleted_at IS NULL
                """, rs -> rs.next() ? rs.getString(1) : null, userId);
        if (customerEmail == null || customerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Active customer account required");
        }

        List<Map<String, Object>> cartItems = jdbc.queryForList("""
                SELECT variant_id,quantity FROM cart_items
                WHERE cart_id=?
                ORDER BY variant_id
                FOR UPDATE
                """, cartId);
        if (cartItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Your cart is empty");
        }

        // Lock every sellable variant and its inventory row in ascending variant ID order.
        // This matches the inventory adjustment lock order and prevents overselling races.
        List<CheckoutLine> lines = new ArrayList<>();
        for (Map<String, Object> cartItem : cartItems) {
            long variantId = number(cartItem.get("variant_id"));
            int quantity = Math.toIntExact(number(cartItem.get("quantity")));
            if (quantity <= 0 || quantity > 99) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "A cart quantity is outside the allowed range");
            }

            Map<String, Object> variant = oneOrNull("""
                    SELECT pv.id AS variant_id,pv.product_id,p.category_id,p.name AS product_name,
                           pv.variant_name,pv.sku,pv.price,pv.is_active AS variant_active,
                           p.status AS product_status,p.deleted_at
                    FROM product_variants pv
                    JOIN products p ON p.id=pv.product_id
                    WHERE pv.id=?
                    FOR UPDATE
                    """, variantId);
            if (variant == null || !"ACTIVE".equals(variant.get("product_status"))
                    || variant.get("deleted_at") != null || !truthy(variant.get("variant_active"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "A cart product or variant is no longer available");
            }

            Map<String, Object> inventory = oneOrNull("""
                    SELECT quantity_on_hand,quantity_reserved
                    FROM inventory WHERE variant_id=? FOR UPDATE
                    """, variantId);
            if (inventory == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Inventory is not configured for a cart item");
            }
            long onHand = number(inventory.get("quantity_on_hand"));
            long reserved = number(inventory.get("quantity_reserved"));
            InventoryQuantity stock = new InventoryQuantity(onHand, reserved, 0);
            long available = stock.availableQuantity();
            if (!stock.canReserve(quantity)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Insufficient stock for " + variant.get("product_name")
                                + ". Available quantity: " + available);
            }

            lines.add(new CheckoutLine(number(variant.get("product_id")),
                    number(variant.get("category_id")), variantId,
                    (String) variant.get("product_name"), (String) variant.get("variant_name"),
                    (String) variant.get("sku"), quantity, (BigDecimal) variant.get("price"), available));
        }

        LockedCoupon lockedCoupon = request.couponCode() == null || request.couponCode().isBlank()
                ? null : lockAndValidateCoupon(request.couponCode().trim(), userId, lines);
        CheckoutQuoteService.Quote quote;
        try {
            quote = quoteService.calculate(lines,
                    lockedCoupon == null ? null : lockedCoupon.coupon(),
                    (String) address.get("country_code"), (String) address.get("city"),
                    (String) address.get("area"));
        } catch (IllegalArgumentException invalidCoupon) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, invalidCoupon.getMessage());
        }

        String orderNumber = orderNumberGenerator.nextOrderNumber();
        LocalDateTime placedAt = LocalDateTime.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC);
        String note = request.customerNote() == null || request.customerNote().isBlank()
                ? null : request.customerNote().trim();

        jdbc.update("""
                INSERT INTO customer_orders(
                    order_number,user_id,order_status,payment_status,currency,
                    subtotal,discount_total,shipping_total,tax_total,grand_total,coupon_code_snapshot,
                    shipping_recipient,shipping_phone,customer_email,
                    shipping_address_line1,shipping_address_line2,shipping_area,shipping_city,
                    shipping_district,shipping_postal_code,shipping_country_code,customer_note,placed_at
                ) VALUES(?,?,'PENDING','UNPAID',?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
                """,
                orderNumber, userId, currency,
                quote.totals().subtotal(), quote.totals().discountTotal(),
                quote.totals().shippingTotal(), quote.totals().taxTotal(), quote.totals().grandTotal(),
                quote.couponCode(), address.get("recipient_name"), address.get("phone"), customerEmail,
                address.get("address_line1"), address.get("address_line2"), address.get("area"),
                address.get("city"), address.get("district"), address.get("postal_code"),
                address.get("country_code"), note, placedAt);
        long orderId = jdbc.queryForObject("SELECT LAST_INSERT_ID()", Long.class);

        List<OrderPlacementResponse.Item> responseItems = new ArrayList<>();
        for (int index = 0; index < lines.size(); index++) {
            CheckoutLine line = lines.get(index);
            CheckoutQuoteService.QuotedLine priced = quote.lines().get(index);
            jdbc.update("""
                    INSERT INTO order_items(
                        order_id,variant_id,product_name_snapshot,variant_name_snapshot,sku_snapshot,
                        unit_price,quantity,line_discount,line_total
                    ) VALUES(?,?,?,?,?,?,?,?,?)
                    """, orderId, line.variantId(), line.productName(), line.variantName(), line.sku(),
                    line.unitPrice(), line.quantity(), priced.lineDiscount(), priced.lineTotal());
            responseItems.add(new OrderPlacementResponse.Item(line.productId(), line.variantId(),
                    line.productName(), line.variantName(), line.sku(), line.quantity(),
                    line.unitPrice(), priced.lineDiscount(), priced.lineTotal()));
        }

        inventoryReservationService.reserve(orderId, orderNumber, lines);

        if (lockedCoupon != null) {
            int updated = jdbc.update("""
                    UPDATE coupons SET usage_count=usage_count+1
                    WHERE id=? AND is_active=TRUE
                      AND (usage_limit IS NULL OR usage_count < usage_limit)
                    """, lockedCoupon.id());
            if (updated != 1) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Coupon usage limit has been reached");
            }
            jdbc.update("""
                    INSERT INTO coupon_redemptions(coupon_id,user_id,order_id,discount_amount)
                    VALUES(?,?,?,?)
                    """, lockedCoupon.id(), userId, orderId, quote.totals().discountTotal());
        }

        jdbc.update("""
                INSERT INTO order_status_history(order_id,from_status,to_status,note,changed_by)
                VALUES(?,NULL,'PENDING','Order placed by customer',?)
                """, orderId, userId);
        notificationService.orderPlaced(userId, orderId);

        int converted = jdbc.update("""
                UPDATE carts SET cart_status='CONVERTED'
                WHERE id=? AND user_id=? AND cart_status='ACTIVE'
                """, cartId, userId);
        if (converted != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cart changed while placing the order. Please try again.");
        }

        String shippingAddress = address.get("address_line1")
                + (address.get("address_line2") == null ? "" : ", " + address.get("address_line2"))
                + ", " + address.get("area") + ", " + address.get("city")
                + ", " + address.get("district") + " " + address.get("postal_code")
                + ", " + address.get("country_code");

        return new OrderPlacementResponse(orderId, orderNumber, "PENDING", "UNPAID", currency,
                quote.totals().subtotal(), quote.totals().discountTotal(),
                quote.totals().shippingTotal(), quote.totals().taxTotal(), quote.totals().grandTotal(),
                quote.couponCode(), (String) address.get("recipient_name"), (String) address.get("phone"),
                shippingAddress, Instant.now(clock), responseItems);
    }

    private LockedCoupon lockAndValidateCoupon(String code, long userId, List<CheckoutLine> lines) {
        Map<String, Object> row = oneOrNull("""
                SELECT id,code,discount_type,discount_value,minimum_order_amount,maximum_discount_amount,
                       usage_limit,per_user_limit,usage_count,starts_at,ends_at,is_active
                FROM coupons WHERE code=? FOR UPDATE
                """, code);
        if (row == null || !truthy(row.get("is_active"))) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Coupon is invalid or unavailable");
        }

        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(clock), java.time.ZoneOffset.UTC);
        LocalDateTime starts = dateTime(row.get("starts_at"));
        LocalDateTime ends = dateTime(row.get("ends_at"));
        if (starts != null && now.isBefore(starts)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon is not active yet");
        }
        if (ends != null && now.isAfter(ends)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon has expired");
        }
        Object usageLimit = row.get("usage_limit");
        if (usageLimit != null && number(row.get("usage_count")) >= number(usageLimit)) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Coupon usage limit has been reached");
        }
        Object perUserLimit = row.get("per_user_limit");
        if (perUserLimit != null) {
            Integer used = jdbc.queryForObject("""
                    SELECT COUNT(*) FROM coupon_redemptions WHERE coupon_id=? AND user_id=?
                    """, Integer.class, row.get("id"), userId);
            if (used != null && used >= number(perUserLimit)) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "You have reached this coupon's usage limit");
            }
        }

        long couponId = number(row.get("id"));
        Set<Long> productIds = new HashSet<>(jdbc.query(
                "SELECT product_id FROM coupon_products WHERE coupon_id=?",
                (rs, index) -> rs.getLong(1), couponId));
        Set<Long> categoryIds = new HashSet<>(jdbc.query(
                "SELECT category_id FROM coupon_categories WHERE coupon_id=?",
                (rs, index) -> rs.getLong(1), couponId));
        boolean restricted = !productIds.isEmpty() || !categoryIds.isEmpty();
        BigDecimal subtotal = lines.stream().map(CheckoutLine::lineSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (subtotal.compareTo((BigDecimal) row.get("minimum_order_amount")) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Cart does not meet the coupon minimum order amount");
        }
        BigDecimal eligibleSubtotal = lines.stream()
                .filter(line -> !restricted || productIds.contains(line.productId())
                        || categoryIds.contains(line.categoryId()))
                .map(CheckoutLine::lineSubtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        if (eligibleSubtotal.signum() <= 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Coupon does not apply to items in your cart");
        }
        Coupon coupon = new Coupon((String) row.get("code"), (String) row.get("discount_type"),
                (BigDecimal) row.get("discount_value"), (BigDecimal) row.get("minimum_order_amount"),
                (BigDecimal) row.get("maximum_discount_amount"), eligibleSubtotal,
                productIds, categoryIds, restricted);
        return new LockedCoupon(couponId, coupon);
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        return value instanceof Number number && number.intValue() != 0;
    }

    private static LocalDateTime dateTime(Object value) {
        if (value == null) return null;
        if (value instanceof java.sql.Timestamp timestamp) return timestamp.toLocalDateTime();
        if (value instanceof LocalDateTime dateTime) return dateTime;
        throw new IllegalStateException("Unexpected coupon date type");
    }

    private record LockedCoupon(long id, Coupon coupon) {}
}

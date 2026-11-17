package com.luxeperfume.backend.cart;

import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@Validated
public class CartWishlistController {
    private final JdbcTemplate jdbc;

    public CartWishlistController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping("/api/v1/cart")
    @Transactional
    public ApiResponse<Map<String,Object>> getCart(Authentication authentication) {
        long userId = userId(authentication);
        long cartId = getOrCreateCart(userId);
        return ApiResponse.success("Cart retrieved", cartView(cartId));
    }

    @PostMapping("/api/v1/cart/items")
    @Transactional
    public ApiResponse<Map<String,Object>> addCartItem(Authentication authentication,
            @Valid @RequestBody CartItemRequest request) {
        long userId = userId(authentication);
        long cartId = getOrCreateCart(userId);
        Map<String,Object> variant = lockSellableVariant(request.variantId());
        long available = lockAvailableStock(request.variantId());
        List<Map<String,Object>> existing = jdbc.queryForList(
                "SELECT id,quantity FROM cart_items WHERE cart_id=? AND variant_id=? FOR UPDATE",
                cartId, request.variantId());
        long current = existing.isEmpty() ? 0 : ((Number) existing.getFirst().get("quantity")).longValue();
        long next = current + request.quantity();
        if (next > 99) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A cart line cannot exceed 99 units");
        if (next > available) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Requested quantity exceeds available stock (" + available + ")");
        if (existing.isEmpty()) {
            jdbc.update("INSERT INTO cart_items(cart_id,variant_id,quantity) VALUES(?,?,?)",
                    cartId, request.variantId(), next);
        } else {
            jdbc.update("UPDATE cart_items SET quantity=? WHERE id=?", next, existing.getFirst().get("id"));
        }
        return ApiResponse.success("Cart item added", cartView(cartId));
    }

    @PutMapping("/api/v1/cart/items/{itemId}")
    @Transactional
    public ApiResponse<Map<String,Object>> updateCartItem(Authentication authentication,
            @PathVariable long itemId, @Valid @RequestBody QuantityRequest request) {
        long userId = userId(authentication);
        long cartId = getOrCreateCart(userId);
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT ci.variant_id FROM cart_items ci JOIN carts c ON c.id=ci.cart_id WHERE ci.id=? AND ci.cart_id=? AND c.user_id=? AND c.cart_status='ACTIVE' FOR UPDATE",
                itemId, cartId, userId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        long variantId = ((Number) rows.getFirst().get("variant_id")).longValue();
        lockSellableVariant(variantId);
        long available = lockAvailableStock(variantId);
        if (request.quantity() > available) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Requested quantity exceeds available stock (" + available + ")");
        jdbc.update("UPDATE cart_items SET quantity=? WHERE id=? AND cart_id=?", request.quantity(), itemId, cartId);
        return ApiResponse.success("Cart quantity updated", cartView(cartId));
    }

    @DeleteMapping("/api/v1/cart/items/{itemId}")
    @Transactional
    public ApiResponse<Map<String,Object>> removeCartItem(Authentication authentication, @PathVariable long itemId) {
        long userId = userId(authentication);
        long cartId = getOrCreateCart(userId);
        int changed = jdbc.update("DELETE FROM cart_items WHERE id=? AND cart_id=?", itemId, cartId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart item not found");
        return ApiResponse.success("Cart item removed", cartView(cartId));
    }

    @GetMapping("/api/v1/wishlist")
    @Transactional
    public ApiResponse<Map<String,Object>> getWishlist(Authentication authentication) {
        long wishlistId = getOrCreateWishlist(userId(authentication));
        return ApiResponse.success("Wishlist retrieved", wishlistView(wishlistId));
    }

    @PostMapping("/api/v1/wishlist/items/{productId}")
    @Transactional
    public ApiResponse<Map<String,Object>> addWishlistItem(Authentication authentication, @PathVariable long productId) {
        long wishlistId = getOrCreateWishlist(userId(authentication));
        Integer active = jdbc.queryForObject(
                "SELECT COUNT(*) FROM products WHERE id=? AND status='ACTIVE' AND deleted_at IS NULL", Integer.class, productId);
        if (active == null || active == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Active product not found");
        jdbc.update("INSERT IGNORE INTO wishlist_items(wishlist_id,product_id) VALUES(?,?)", wishlistId, productId);
        return ApiResponse.success("Wishlist item added", wishlistView(wishlistId));
    }

    @DeleteMapping("/api/v1/wishlist/items/{productId}")
    @Transactional
    public ApiResponse<Map<String,Object>> removeWishlistItem(Authentication authentication, @PathVariable long productId) {
        long wishlistId = getOrCreateWishlist(userId(authentication));
        int changed = jdbc.update("DELETE FROM wishlist_items WHERE wishlist_id=? AND product_id=?", wishlistId, productId);
        if (changed == 0) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Wishlist item not found");
        return ApiResponse.success("Wishlist item removed", wishlistView(wishlistId));
    }

    private Map<String,Object> cartView(long cartId) {
        Map<String,Object> cart = jdbc.queryForMap("SELECT id,user_id,currency,created_at,updated_at FROM carts WHERE id=? AND cart_status='ACTIVE'", cartId);
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT ci.id AS item_id,ci.variant_id,ci.quantity,ci.added_at,
                       p.id AS product_id,p.name AS product_name,p.slug AS product_slug,p.status AS product_status,
                       pv.variant_name,pv.sku,pv.price AS current_unit_price,pv.is_active AS variant_active,
                       COALESCE(i.quantity_on_hand,0) AS quantity_on_hand,
                       COALESCE(i.quantity_reserved,0) AS quantity_reserved,
                       GREATEST(COALESCE(i.quantity_on_hand,0)-COALESCE(i.quantity_reserved,0),0) AS available_stock,
                       pi.image_url
                FROM cart_items ci
                JOIN product_variants pv ON pv.id=ci.variant_id
                JOIN products p ON p.id=pv.product_id
                LEFT JOIN inventory i ON i.variant_id=pv.id
                LEFT JOIN product_images pi ON pi.product_id=p.id AND pi.is_primary=TRUE
                WHERE ci.cart_id=?
                ORDER BY ci.added_at DESC,ci.id DESC
                """, cartId);
        List<Map<String,Object>> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        long itemCount = 0;
        boolean allAvailable = true;
        for (Map<String,Object> row : rows) {
            Map<String,Object> item = new LinkedHashMap<>(row);
            BigDecimal unitPrice = (BigDecimal) row.get("current_unit_price");
            long quantity = ((Number) row.get("quantity")).longValue();
            long available = ((Number) row.get("available_stock")).longValue();
            boolean active = "ACTIVE".equals(row.get("product_status")) && truthy(row.get("variant_active"));
            boolean stockValid = active && available >= quantity;
            BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(quantity));
            item.put("currentUnitPrice", unitPrice);
            item.put("lineTotal", lineTotal);
            item.put("stockValid", stockValid);
            item.put("availableStock", available);
            item.put("productActive", "ACTIVE".equals(row.get("product_status")));
            item.put("variantActive", truthy(row.get("variant_active")));
            item.remove("current_unit_price");
            item.remove("available_stock");
            item.remove("product_status");
            item.remove("variant_active");
            items.add(item);
            subtotal = subtotal.add(lineTotal);
            itemCount += quantity;
            allAvailable &= stockValid;
        }
        cart.put("items", items);
        cart.put("subtotal", subtotal);
        cart.put("itemCount", itemCount);
        cart.put("lineCount", items.size());
        cart.put("allItemsAvailable", allAvailable);
        return cart;
    }

    private Map<String,Object> wishlistView(long wishlistId) {
        Map<String,Object> wishlist = jdbc.queryForMap("SELECT id,user_id,name,created_at,updated_at FROM wishlists WHERE id=?", wishlistId);
        List<Map<String,Object>> items = jdbc.queryForList("""
                SELECT p.id AS product_id,p.name,p.slug,p.short_description,
                       (SELECT MIN(v.price) FROM product_variants v WHERE v.product_id=p.id AND v.is_active=TRUE) AS starting_price,
                       (SELECT pi.image_url FROM product_images pi WHERE pi.product_id=p.id ORDER BY pi.is_primary DESC,pi.sort_order,pi.id LIMIT 1) AS image_url,
                       wi.added_at
                FROM wishlist_items wi JOIN products p ON p.id=wi.product_id
                WHERE wi.wishlist_id=? ORDER BY wi.added_at DESC
                """, wishlistId);
        wishlist.put("items", items);
        wishlist.put("itemCount", items.size());
        return wishlist;
    }

    private Map<String,Object> lockSellableVariant(long variantId) {
        List<Map<String,Object>> rows = jdbc.queryForList("""
                SELECT pv.id,pv.price FROM product_variants pv
                JOIN products p ON p.id=pv.product_id
                WHERE pv.id=? AND pv.is_active=TRUE AND p.status='ACTIVE' AND p.deleted_at IS NULL
                FOR UPDATE
                """, variantId);
        if (rows.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT, "Product or variant is no longer available");
        return rows.getFirst();
    }

    private long lockAvailableStock(long variantId) {
        List<Map<String,Object>> rows = jdbc.queryForList(
                "SELECT quantity_on_hand,quantity_reserved FROM inventory WHERE variant_id=? FOR UPDATE", variantId);
        if (rows.isEmpty()) return 0;
        long onHand = ((Number) rows.getFirst().get("quantity_on_hand")).longValue();
        long reserved = ((Number) rows.getFirst().get("quantity_reserved")).longValue();
        return Math.max(0, onHand - reserved);
    }

    private long getOrCreateCart(long userId) {
        List<Long> ids = jdbc.query("SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'",
                (rs, n) -> rs.getLong(1), userId);
        if (!ids.isEmpty()) return ids.getFirst();
        try {
            jdbc.update("INSERT INTO carts(user_id,cart_status,currency) VALUES(?,'ACTIVE','BDT')", userId);
        } catch (DuplicateKeyException concurrentCreate) {
            // Another request created the unique active cart for this user.
        }
        return jdbc.queryForObject("SELECT id FROM carts WHERE user_id=? AND cart_status='ACTIVE'", Long.class, userId);
    }

    private long getOrCreateWishlist(long userId) {
        List<Long> ids = jdbc.query("SELECT id FROM wishlists WHERE user_id=? AND name='My Wishlist'",
                (rs, n) -> rs.getLong(1), userId);
        if (!ids.isEmpty()) return ids.getFirst();
        try {
            jdbc.update("INSERT INTO wishlists(user_id,name) VALUES(?,'My Wishlist')", userId);
        } catch (DuplicateKeyException concurrentCreate) {
            // Another request created the default wishlist for this user.
        }
        return jdbc.queryForObject("SELECT id FROM wishlists WHERE user_id=? AND name='My Wishlist'", Long.class, userId);
    }

    private static long userId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return principal.longValue();
    }

    private static boolean truthy(Object value) {
        if (value instanceof Boolean bool) return bool;
        return value instanceof Number number && number.intValue() != 0;
    }

    public record CartItemRequest(@NotNull Long variantId, @NotNull @Min(1) @Max(99) Integer quantity) {}
    public record QuantityRequest(@NotNull @Min(1) @Max(99) Integer quantity) {}
}

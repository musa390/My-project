package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.OrderCancellationRequest;
import com.luxeperfume.backend.checkout.policy.OrderCancellationPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class CustomerOrderApplicationService {
    private static final int MAX_PAGE_SIZE = 50;
    private final JdbcTemplate jdbc;
    private final OrderCancellationPolicy cancellationPolicy;

    public CustomerOrderApplicationService(JdbcTemplate jdbc, OrderCancellationPolicy cancellationPolicy) {
        this.jdbc = jdbc;
        this.cancellationPolicy = cancellationPolicy;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> history(long customerId, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        long offset = (long) safePage * safeSize;
        Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM customer_orders WHERE user_id=?", Integer.class, customerId);
        List<Map<String, Object>> orders = jdbc.queryForList("""
                SELECT o.id,o.order_number,o.order_status,o.payment_status,o.currency,
                       o.subtotal,o.discount_total,o.shipping_total,o.tax_total,o.grand_total,
                       o.placed_at,o.created_at,
                       (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id=o.id) AS line_count,
                       (SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi WHERE oi.order_id=o.id) AS item_count
                FROM customer_orders o
                WHERE o.user_id=?
                ORDER BY COALESCE(o.placed_at,o.created_at) DESC,o.id DESC
                LIMIT ? OFFSET ?
                """, customerId, safeSize, offset);
        long total = count == null ? 0 : count;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", orders);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", total);
        result.put("totalPages", total == 0 ? 0 : (total + safeSize - 1) / safeSize);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(long customerId, String orderNumber) {
        Map<String, Object> order = ownedOrder(customerId, orderNumber);
        long orderId = number(order.get("id"));
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT id,variant_id,product_name_snapshot,variant_name_snapshot,sku_snapshot,
                       unit_price,quantity,line_discount,line_total,created_at
                FROM order_items WHERE order_id=? ORDER BY id
                """, orderId);
        List<Map<String, Object>> history = statusHistory(orderId);
        order.put("items", items);
        order.put("statusHistory", history);
        return order;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> tracking(long customerId, String orderNumber) {
        Map<String, Object> order = ownedOrder(customerId, orderNumber);
        Map<String, Object> tracking = new LinkedHashMap<>();
        tracking.put("orderNumber", order.get("order_number"));
        tracking.put("currentStatus", order.get("order_status"));
        tracking.put("placedAt", order.get("placed_at"));
        tracking.put("updatedAt", order.get("updated_at"));
        tracking.put("events", statusHistory(number(order.get("id"))));
        return tracking;
    }

    /**
     * Order row locking serializes duplicate cancellation requests. Inventory
     * rows are then locked in ascending variant ID order. The order transition,
     * reservation release, ledger entries and audit event commit or roll back
     * together, so a retry cannot release the same reservation twice.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> cancel(long customerId, String orderNumber,
                                      OrderCancellationRequest request) {
        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,order_status,payment_status
                FROM customer_orders
                WHERE order_number=? AND user_id=?
                FOR UPDATE
                """, orderNumber, customerId);
        if (order == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        }

        String fromStatus = (String) order.get("order_status");
        String paymentStatus = (String) order.get("payment_status");
        if (!cancellationPolicy.mayCustomerCancel(fromStatus, paymentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This order can no longer be cancelled online");
        }

        long orderId = number(order.get("id"));
        List<Map<String, Object>> lines = jdbc.queryForList("""
                SELECT variant_id,quantity
                FROM order_items
                WHERE order_id=?
                ORDER BY variant_id,id
                FOR UPDATE
                """, orderId);
        List<Map<String, Object>> released = new ArrayList<>();
        for (Map<String, Object> line : lines) {
            Object variantValue = line.get("variant_id");
            if (variantValue == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "An order item has no inventory reference; support review is required");
            }
            long variantId = number(variantValue);
            long quantity = number(line.get("quantity"));
            Map<String, Object> inventory = oneOrNull("""
                    SELECT quantity_reserved
                    FROM inventory WHERE variant_id=?
                    FOR UPDATE
                    """, variantId);
            if (inventory == null || number(inventory.get("quantity_reserved")) < quantity) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved stock is inconsistent; cancellation was not applied");
            }
            int changed = jdbc.update("""
                    UPDATE inventory
                    SET quantity_reserved=quantity_reserved-?
                    WHERE variant_id=? AND quantity_reserved>=?
                    """, quantity, variantId, quantity);
            if (changed != 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Reserved stock changed; cancellation was not applied");
            }
            jdbc.update("""
                    INSERT INTO inventory_movements
                        (variant_id,movement_type,quantity_change,reference_type,reference_id,note)
                    VALUES(?,'RELEASE',?,'ORDER',?,?)
                    """, variantId, -quantity, orderId,
                    "Released reservation for cancelled order " + orderNumber);
            Map<String, Object> releasedLine = new LinkedHashMap<>();
            releasedLine.put("variantId", variantId);
            releasedLine.put("quantityReleased", quantity);
            released.add(releasedLine);
        }

        int transitioned = jdbc.update("""
                UPDATE customer_orders
                SET order_status='CANCELLED'
                WHERE id=? AND order_status=? AND payment_status IN ('UNPAID','FAILED')
                """, orderId, fromStatus);
        if (transitioned != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Order status changed; refresh and try again");
        }

        String reason = request == null || request.reason() == null || request.reason().isBlank()
                ? "Cancelled by customer" : request.reason().trim();
        jdbc.update("""
                INSERT INTO order_status_history(order_id,from_status,to_status,note,changed_by)
                VALUES(?,?,'CANCELLED',?,?)
                """, orderId, fromStatus, reason, customerId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNumber", orderNumber);
        result.put("previousStatus", fromStatus);
        result.put("orderStatus", "CANCELLED");
        result.put("releasedInventory", released);
        result.put("message", "Order cancelled successfully");
        return result;
    }

    private Map<String, Object> ownedOrder(long customerId, String orderNumber) {
        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,order_status,payment_status,currency,
                       subtotal,discount_total,shipping_total,tax_total,grand_total,
                       coupon_code_snapshot,shipping_recipient,shipping_phone,customer_email,
                       shipping_address_line1,shipping_address_line2,shipping_area,shipping_city,
                       shipping_district,shipping_postal_code,shipping_country_code,customer_note,
                       placed_at,created_at,updated_at
                FROM customer_orders
                WHERE order_number=? AND user_id=?
                """, orderNumber, customerId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        return order;
    }

    private List<Map<String, Object>> statusHistory(long orderId) {
        return jdbc.queryForList("""
                SELECT from_status,to_status,note,changed_by,created_at
                FROM order_status_history
                WHERE order_id=?
                ORDER BY created_at,id
                """, orderId);
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }
}

package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.OrderStatusUpdateRequest;
import com.luxeperfume.backend.checkout.policy.OrderStatusTransitionPolicy;
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
public class StaffOrderApplicationService {
    private static final int MAX_PAGE_SIZE = 100;
    private final JdbcTemplate jdbc;
    private final OrderStatusTransitionPolicy transitionPolicy;

    public StaffOrderApplicationService(JdbcTemplate jdbc, OrderStatusTransitionPolicy transitionPolicy) {
        this.jdbc = jdbc;
        this.transitionPolicy = transitionPolicy;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> search(String status, String paymentStatus, String query, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(MAX_PAGE_SIZE, Math.max(1, size));
        long offset = (long) safePage * safeSize;
        StringBuilder where = new StringBuilder(" WHERE 1=1 ");
        List<Object> args = new ArrayList<>();
        if (status != null && !status.isBlank()) {
            where.append(" AND o.order_status=? ");
            args.add(status.trim().toUpperCase());
        }
        if (paymentStatus != null && !paymentStatus.isBlank()) {
            where.append(" AND o.payment_status=? ");
            args.add(paymentStatus.trim().toUpperCase());
        }
        if (query != null && !query.isBlank()) {
            where.append(" AND (o.order_number LIKE ? OR o.customer_email LIKE ? OR o.shipping_recipient LIKE ? OR o.shipping_phone LIKE ?) ");
            String term = "%" + query.trim() + "%";
            args.add(term); args.add(term); args.add(term); args.add(term);
        }

        Integer count = jdbc.queryForObject("SELECT COUNT(*) FROM customer_orders o " + where, Integer.class,
                args.toArray());
        List<Object> pageArgs = new ArrayList<>(args);
        pageArgs.add(safeSize);
        pageArgs.add(offset);
        List<Map<String, Object>> items = jdbc.queryForList("""
                SELECT o.id,o.order_number,o.user_id,o.order_status,o.payment_status,o.currency,
                       o.subtotal,o.discount_total,o.shipping_total,o.tax_total,o.grand_total,
                       o.shipping_recipient,o.shipping_phone,o.customer_email,o.shipping_city,
                       o.shipping_district,o.placed_at,o.created_at,o.updated_at,
                       (SELECT COUNT(*) FROM order_items oi WHERE oi.order_id=o.id) AS line_count,
                       (SELECT COALESCE(SUM(oi.quantity),0) FROM order_items oi WHERE oi.order_id=o.id) AS item_count
                FROM customer_orders o
                """ + where + " ORDER BY COALESCE(o.placed_at,o.created_at) DESC,o.id DESC LIMIT ? OFFSET ?",
                pageArgs.toArray());

        long total = count == null ? 0 : count;
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("items", items);
        result.put("page", safePage);
        result.put("size", safeSize);
        result.put("totalElements", total);
        result.put("totalPages", total == 0 ? 0 : (total + safeSize - 1) / safeSize);
        return result;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> detail(String orderNumber) {
        Map<String, Object> order = findOrder(orderNumber);
        long orderId = number(order.get("id"));
        order.put("items", jdbc.queryForList("""
                SELECT id,variant_id,product_name_snapshot,variant_name_snapshot,sku_snapshot,
                       unit_price,quantity,line_discount,line_total,created_at
                FROM order_items WHERE order_id=? ORDER BY id
                """, orderId));
        order.put("statusHistory", jdbc.queryForList("""
                SELECT from_status,to_status,note,changed_by,created_at
                FROM order_status_history WHERE order_id=? ORDER BY created_at,id
                """, orderId));
        order.put("payments", jdbc.queryForList("""
                SELECT id,provider,payment_method,provider_transaction_id,provider_reference,
                       amount,currency,payment_status,initiated_at,completed_at,created_at
                FROM payments WHERE order_id=? ORDER BY created_at,id
                """, orderId));
        return order;
    }

    /**
     * Locks the order before changing its lifecycle. Cancellation releases each
     * reservation and writes its ledger movement in the same transaction as
     * the status/audit update. A failed step rolls the entire operation back.
     */
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> updateStatus(String orderNumber, long actorId, OrderStatusUpdateRequest request) {
        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,order_status,payment_status
                FROM customer_orders WHERE order_number=? FOR UPDATE
                """, orderNumber);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");

        long orderId = number(order.get("id"));
        String current = (String) order.get("order_status");
        String target = request.status().trim().toUpperCase();
        String payment = (String) order.get("payment_status");

        if (!transitionPolicy.mayTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid order status transition: " + current + " -> " + target);
        }

        List<Map<String, Object>> released = new ArrayList<>();
        if ("CANCELLED".equals(target)) {
            if (!"UNPAID".equals(payment) && !"FAILED".equals(payment)) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Paid or pending-payment orders require payment/refund handling before cancellation");
            }
            List<Map<String, Object>> lines = jdbc.queryForList("""
                    SELECT variant_id,quantity FROM order_items
                    WHERE order_id=? ORDER BY variant_id,id FOR UPDATE
                    """, orderId);
            for (Map<String, Object> line : lines) {
                if (line.get("variant_id") == null) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "An order item has no inventory reference; manual review is required");
                }
                long variantId = number(line.get("variant_id"));
                long quantity = number(line.get("quantity"));
                Map<String, Object> inventory = oneOrNull("""
                        SELECT quantity_reserved FROM inventory WHERE variant_id=? FOR UPDATE
                        """, variantId);
                if (inventory == null || number(inventory.get("quantity_reserved")) < quantity) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Reserved stock is inconsistent; status was not changed");
                }
                int changed = jdbc.update("""
                        UPDATE inventory SET quantity_reserved=quantity_reserved-?
                        WHERE variant_id=? AND quantity_reserved>=?
                        """, quantity, variantId, quantity);
                if (changed != 1) {
                    throw new ResponseStatusException(HttpStatus.CONFLICT,
                            "Reserved stock changed; status was not changed");
                }
                jdbc.update("""
                        INSERT INTO inventory_movements
                            (variant_id,movement_type,quantity_change,reference_type,reference_id,note)
                        VALUES(?,'RELEASE',?,'ORDER',?,?)
                        """, variantId, -quantity, orderId,
                        "Released reservation by staff for cancelled order " + orderNumber);
                Map<String, Object> release = new LinkedHashMap<>();
                release.put("variantId", variantId);
                release.put("quantityReleased", quantity);
                released.add(release);
            }
        }

        int changed = jdbc.update("""
                UPDATE customer_orders SET order_status=?
                WHERE id=? AND order_status=?
                """, target, orderId, current);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Order status changed concurrently");

        String note = request.note() == null || request.note().isBlank()
                ? "Status updated by staff" : request.note().trim();
        jdbc.update("""
                INSERT INTO order_status_history(order_id,from_status,to_status,note,changed_by)
                VALUES(?,?,?,?,?)
                """, orderId, current, target, note, actorId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNumber", orderNumber);
        result.put("previousStatus", current);
        result.put("orderStatus", target);
        result.put("releasedInventory", released);
        result.put("message", "Order status updated successfully");
        return result;
    }

    private Map<String, Object> findOrder(String orderNumber) {
        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,user_id,order_status,payment_status,currency,
                       subtotal,discount_total,shipping_total,tax_total,grand_total,
                       coupon_code_snapshot,shipping_recipient,shipping_phone,customer_email,
                       shipping_address_line1,shipping_address_line2,shipping_area,shipping_city,
                       shipping_district,shipping_postal_code,shipping_country_code,customer_note,
                       placed_at,created_at,updated_at
                FROM customer_orders WHERE order_number=?
                """, orderNumber);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        return order;
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }
}

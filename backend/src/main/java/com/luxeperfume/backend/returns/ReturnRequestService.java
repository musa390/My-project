package com.luxeperfume.backend.returns;

import com.luxeperfume.backend.checkout.policy.OrderStatusTransitionPolicy;
import com.luxeperfume.backend.returns.domain.ReturnEligibilityPolicy;
import com.luxeperfume.backend.returns.domain.ReturnStatus;
import com.luxeperfume.backend.returns.domain.ReturnStatusTransitionPolicy;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * [ENC] Owns the full-order return-request lifecycle. Return receipt never
 * automatically restocks inventory or marks a payment refunded.
 */
@Service
public class ReturnRequestService {
    private final JdbcTemplate jdbc;
    private final ReturnStatusTransitionPolicy transitionPolicy;
    private final OrderStatusTransitionPolicy orderTransitionPolicy;

    public ReturnRequestService(JdbcTemplate jdbc, ReturnStatusTransitionPolicy transitionPolicy,
                                OrderStatusTransitionPolicy orderTransitionPolicy) {
        this.jdbc = jdbc;
        this.transitionPolicy = transitionPolicy;
        this.orderTransitionPolicy = orderTransitionPolicy;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestReturn(long userId, String orderNumber,
                                             String reason, String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key is required");
        }
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A return reason (1-500 chars) is required");
        }

        Map<String, Object> duplicate = oneOrNull("""
                SELECT id,user_id,order_id,reason FROM return_requests WHERE idempotency_key=?
                """, idempotencyKey);
        if (duplicate != null) {
            if (number(duplicate.get("user_id")) != userId
                    || !reason.trim().equals(duplicate.get("reason"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used for another return request");
            }
            Map<String, Object> ownedOrder = oneOrNull("""
                    SELECT id FROM customer_orders WHERE id=? AND order_number=? AND user_id=?
                    """, duplicate.get("order_id"), orderNumber, userId);
            if (ownedOrder == null) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key belongs to a different order");
            }
            return detail(number(duplicate.get("id")));
        }

        Map<String, Object> order = oneOrNull("""
                SELECT id,order_status FROM customer_orders
                WHERE order_number=? AND user_id=? FOR UPDATE
                """, orderNumber, userId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        try {
            ReturnEligibilityPolicy.requireEligible((String) order.get("order_status"));
        } catch (IllegalArgumentException ineligible) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ineligible.getMessage());
        }

        long orderId = number(order.get("id"));
        if (oneOrNull("SELECT id FROM return_requests WHERE order_id=? FOR UPDATE", orderId) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A return request already exists for this order");
        }

        String reference = "RET-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO return_requests
                    (order_id,user_id,return_reference,idempotency_key,return_status,reason,requested_by,requested_at)
                VALUES(?,?,?,?,'REQUESTED',?,?,CURRENT_TIMESTAMP)
                """, orderId, userId, reference, idempotencyKey, reason.trim(), userId);
        long returnId = jdbc.queryForObject(
                "SELECT id FROM return_requests WHERE return_reference=?", Long.class, reference);
        addEvent(returnId, null, ReturnStatus.REQUESTED, reason.trim(), userId);
        return detail(returnId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerRequests(long userId, String orderNumber) {
        Map<String, Object> order = oneOrNull("""
                SELECT id FROM customer_orders WHERE order_number=? AND user_id=?
                """, orderNumber, userId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        List<Long> ids = jdbc.query("""
                SELECT id FROM return_requests WHERE order_id=? ORDER BY requested_at DESC,id DESC
                """, (rs, row) -> rs.getLong(1), number(order.get("id")));
        return ids.stream().map(this::detail).toList();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> staffQueue(String status, int page, int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));
        long offset = (long) safePage * safeSize;
        String normalized = status == null || status.isBlank() ? null : status.trim().toUpperCase(Locale.ROOT);
        if (normalized != null) {
            try { ReturnStatus.valueOf(normalized); }
            catch (IllegalArgumentException invalid) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid return status filter");
            }
        }
        List<Long> ids = normalized == null
                ? jdbc.query("SELECT id FROM return_requests ORDER BY requested_at DESC,id DESC LIMIT ? OFFSET ?",
                    (rs, row) -> rs.getLong(1), safeSize, offset)
                : jdbc.query("SELECT id FROM return_requests WHERE return_status=? ORDER BY requested_at DESC,id DESC LIMIT ? OFFSET ?",
                    (rs, row) -> rs.getLong(1), normalized, safeSize, offset);
        return ids.stream().map(this::detail).toList();
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> decide(long returnId, long staffId, String decision, String note) {
        ReturnStatus target;
        try { target = ReturnStatus.valueOf(decision == null ? "" : decision.trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        if (target != ReturnStatus.APPROVED && target != ReturnStatus.REJECTED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Decision must be APPROVED or REJECTED");
        }
        Map<String, Object> request = oneOrNull("""
                SELECT id,return_status FROM return_requests WHERE id=? FOR UPDATE
                """, returnId);
        if (request == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found");
        ReturnStatus current = ReturnStatus.valueOf((String) request.get("return_status"));
        requireTransition(current, target);
        int changed = jdbc.update("""
                UPDATE return_requests SET return_status=?,processed_by=?,processed_at=CURRENT_TIMESTAMP
                WHERE id=? AND return_status=?
                """, target.name(), staffId, returnId, current.name());
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT, "Return request changed concurrently");
        addEvent(returnId, current, target, safeNote(note, "Return request " + target.name().toLowerCase(Locale.ROOT)), staffId);
        return detail(returnId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> markReceived(long returnId, long staffId, String note) {
        Map<String, Object> reference = oneOrNull(
                "SELECT id,order_id FROM return_requests WHERE id=?", returnId);
        if (reference == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found");

        // Lock order first, then return row, matching request creation's lock order.
        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,order_status FROM customer_orders WHERE id=? FOR UPDATE
                """, number(reference.get("order_id")));
        Map<String, Object> request = oneOrNull("""
                SELECT id,return_status FROM return_requests WHERE id=? FOR UPDATE
                """, returnId);
        if (order == null || request == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found");
        ReturnStatus current = ReturnStatus.valueOf((String) request.get("return_status"));
        requireTransition(current, ReturnStatus.RECEIVED);
        if (!orderTransitionPolicy.mayTransition((String) order.get("order_status"), "RETURNED")) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "The order is no longer in a returnable delivered state");
        }

        int requestChanged = jdbc.update("""
                UPDATE return_requests SET return_status='RECEIVED',received_at=CURRENT_TIMESTAMP
                WHERE id=? AND return_status='APPROVED'
                """, returnId);
        int orderChanged = jdbc.update("""
                UPDATE customer_orders SET order_status='RETURNED'
                WHERE id=? AND order_status='DELIVERED'
                """, number(order.get("id")));
        if (requestChanged != 1 || orderChanged != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Return receipt could not be committed");
        }
        String eventNote = safeNote(note, "Returned items received for " + order.get("order_number"));
        addEvent(returnId, current, ReturnStatus.RECEIVED, eventNote, staffId);
        jdbc.update("""
                INSERT INTO order_status_history(order_id,from_status,to_status,note,changed_by)
                VALUES(?,'DELIVERED','RETURNED',?,?)
                """, number(order.get("id")), eventNote, staffId);
        return detail(returnId);
    }

    private void requireTransition(ReturnStatus current, ReturnStatus target) {
        if (!transitionPolicy.canTransition(current, target)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Invalid return transition: " + current + " -> " + target);
        }
    }

    private void addEvent(long returnId, ReturnStatus from, ReturnStatus to, String note, long actorId) {
        jdbc.update("""
                INSERT INTO return_request_events(return_request_id,from_status,to_status,note,changed_by)
                VALUES(?,?,?,?,?)
                """, returnId, from == null ? null : from.name(), to.name(), note, actorId);
    }

    private Map<String, Object> detail(long returnId) {
        Map<String, Object> row = oneOrNull("""
                SELECT r.id,r.order_id,o.order_number,r.user_id,r.return_reference,r.return_status,
                       r.reason,r.requested_by,r.processed_by,r.requested_at,r.processed_at,r.received_at,
                       r.created_at,r.updated_at
                FROM return_requests r JOIN customer_orders o ON o.id=r.order_id
                WHERE r.id=?
                """, returnId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Return request not found");
        Map<String, Object> result = camelCase(row);
        result.put("events", jdbc.queryForList("""
                SELECT from_status,to_status,note,changed_by,created_at
                FROM return_request_events WHERE return_request_id=? ORDER BY created_at,id
                """, returnId).stream().map(ReturnRequestService::camelCase).toList());
        return result;
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static Map<String, Object> camelCase(Map<String, Object> row) {
        Map<String, Object> result = new LinkedHashMap<>();
        row.forEach((key, value) -> {
            StringBuilder name = new StringBuilder();
            boolean upper = false;
            for (char character : key.toCharArray()) {
                if (character == '_') upper = true;
                else { name.append(upper ? Character.toUpperCase(character) : character); upper = false; }
            }
            result.put(name.toString(), value);
        });
        return result;
    }

    private static long number(Object value) { return ((Number) value).longValue(); }

    private static String safeNote(String note, String fallback) {
        if (note == null || note.isBlank()) return fallback;
        String trimmed = note.trim();
        if (trimmed.length() > 500) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Return note cannot exceed 500 characters");
        return trimmed;
    }
}

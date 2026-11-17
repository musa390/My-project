package com.luxeperfume.backend.payment;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Transactional COD lifecycle using the existing payments table. */
@Service
public class PaymentApplicationService {
    private final JdbcTemplate jdbc;

    public PaymentApplicationService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> initiateCod(long customerId, String orderNumber, String idempotencyKey) {
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid Idempotency-Key header (8-100 safe characters) is required");
        }

        Map<String, Object> previous = oneOrNull("""
                SELECT p.id,p.order_id,o.order_number,p.provider,p.payment_method,p.amount,p.currency,
                       p.payment_status,p.merchant_reference,p.created_at
                FROM payments p JOIN customer_orders o ON o.id=p.order_id
                WHERE p.idempotency_key=? AND o.user_id=?
                """, idempotencyKey, customerId);
        if (previous != null) {
            if (!orderNumber.equals(previous.get("order_number"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used for another order");
            }
            return paymentView(previous);
        }

        Map<String, Object> order = oneOrNull("""
                SELECT id,order_number,order_status,payment_status,grand_total,currency
                FROM customer_orders
                WHERE order_number=? AND user_id=?
                FOR UPDATE
                """, orderNumber, customerId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");

        String orderStatus = (String) order.get("order_status");
        String paymentStatus = (String) order.get("payment_status");
        if ("CANCELLED".equals(orderStatus) || "RETURNED".equals(orderStatus)
                || !("UNPAID".equals(paymentStatus) || "FAILED".equals(paymentStatus))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "This order is not eligible for a new COD payment attempt");
        }

        Long existing = jdbc.query("""
                SELECT id FROM payments
                WHERE order_id=? AND provider='COD'
                  AND payment_status IN ('INITIATED','PENDING','AUTHORIZED','PAID')
                ORDER BY id DESC LIMIT 1
                """, rs -> rs.next() ? rs.getLong(1) : null, number(order.get("id")));
        if (existing != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "An active COD payment record already exists for this order");
        }

        String merchantReference = "COD-" + UUID.randomUUID();
        BigDecimal amount = (BigDecimal) order.get("grand_total");
        String currency = (String) order.get("currency");
        long orderId = number(order.get("id"));

        jdbc.update("""
                INSERT INTO payments
                    (order_id,provider,payment_method,provider_reference,merchant_reference,
                     idempotency_key,amount,currency,payment_status)
                VALUES(?,'COD','CASH_ON_DELIVERY',?,?,?,?,?,'PENDING')
                """, orderId, merchantReference, merchantReference, idempotencyKey, amount, currency);

        Long paymentId = jdbc.queryForObject(
                "SELECT id FROM payments WHERE idempotency_key=?", Long.class, idempotencyKey);
        int changed = jdbc.update("""
                UPDATE customer_orders SET payment_status='PENDING'
                WHERE id=? AND payment_status IN ('UNPAID','FAILED')
                """, orderId);
        if (changed != 1) throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Order payment status changed; retry with a new request");

        jdbc.update("""
                INSERT INTO payment_events
                    (payment_id,provider,event_type,event_idempotency_key,
                     verification_status,processing_note,processed_at)
                VALUES(?,'COD','COD_REQUESTED',?,'VERIFIED','COD collection is due on delivery',CURRENT_TIMESTAMP)
                """, paymentId, "COD-REQUESTED-" + paymentId);

        return detail(paymentId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmCodCollection(long paymentId, long staffId) {
        Map<String, Object> payment = oneOrNull("""
                SELECT p.id,p.order_id,p.provider,p.payment_status,p.amount,p.currency,
                       p.merchant_reference,o.order_number,o.payment_status AS order_payment_status
                FROM payments p JOIN customer_orders o ON o.id=p.order_id
                WHERE p.id=? FOR UPDATE
                """, paymentId);
        if (payment == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        if (!"COD".equals(payment.get("provider"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only COD payments can be collected here");
        }
        if ("PAID".equals(payment.get("payment_status"))) return detail(paymentId);
        if (!"PENDING".equals(payment.get("payment_status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "COD payment is not awaiting collection");
        }

        int paymentChanged = jdbc.update("""
                UPDATE payments SET payment_status='PAID',completed_at=CURRENT_TIMESTAMP,
                    verified_at=CURRENT_TIMESTAMP
                WHERE id=? AND provider='COD' AND payment_status='PENDING'
                """, paymentId);
        int orderChanged = jdbc.update("""
                UPDATE customer_orders SET payment_status='PAID'
                WHERE id=? AND payment_status='PENDING'
                """, number(payment.get("order_id")));
        if (paymentChanged != 1 || orderChanged != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment/order state changed; collection was not confirmed");
        }

        jdbc.update("""
                INSERT INTO payment_events
                    (payment_id,provider,event_type,event_idempotency_key,
                     verification_status,processing_note,processed_at)
                VALUES(?,'COD','COD_COLLECTED',?,'VERIFIED',?,CURRENT_TIMESTAMP)
                """, paymentId, "COD-COLLECTED-" + paymentId,
                "COD collection confirmed by staff user " + staffId);
        return detail(paymentId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> customerHistory(long customerId, String orderNumber) {
        Map<String, Object> order = oneOrNull("""
                SELECT id FROM customer_orders WHERE order_number=? AND user_id=?
                """, orderNumber, customerId);
        if (order == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found");
        return jdbc.queryForList("""
                SELECT p.id,p.provider,p.payment_method,p.provider_reference,p.merchant_reference,
                       p.amount,p.currency,p.payment_status,p.initiated_at,p.completed_at,p.verified_at,p.created_at
                FROM payments p WHERE p.order_id=? ORDER BY p.created_at DESC,p.id DESC
                """, number(order.get("id"))).stream().map(PaymentApplicationService::camelCase).toList();
    }

    private Map<String, Object> detail(long paymentId) {
        Map<String, Object> row = oneOrNull("""
                SELECT p.id,p.order_id,o.order_number,p.provider,p.payment_method,p.provider_reference,
                       p.merchant_reference,p.amount,p.currency,p.payment_status,p.initiated_at,
                       p.completed_at,p.verified_at,p.created_at
                FROM payments p JOIN customer_orders o ON o.id=p.order_id WHERE p.id=?
                """, paymentId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        return paymentView(row);
    }

    private static Map<String, Object> paymentView(Map<String, Object> row) {
        return camelCase(row);
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
            for (char c : key.toCharArray()) {
                if (c == '_') upper = true;
                else { name.append(upper ? Character.toUpperCase(c) : c); upper = false; }
            }
            result.put(name.toString(), value);
        });
        return result;
    }

    private static long number(Object value) {
        return ((Number) value).longValue();
    }
}

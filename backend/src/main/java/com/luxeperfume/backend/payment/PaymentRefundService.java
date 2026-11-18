package com.luxeperfume.backend.payment;

import com.luxeperfume.backend.payment.domain.RefundAmountPolicy;
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

/**
 * Auditable manual-review refund workflow.
 * It never claims that money was returned until an authorized staff member records
 * an independently completed refund with a reconciliation reference.
 */
@Service
public class PaymentRefundService {
    private final JdbcTemplate jdbc;

    public PaymentRefundService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> requestRefund(long paymentId, long staffId,
                                             BigDecimal amount, String currency,
                                             String reason, String idempotencyKey) {
        validateAmount(amount);
        if (currency == null || !currency.matches("[A-Z]{3}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid ISO currency is required");
        }
        if (idempotencyKey == null || !idempotencyKey.matches("[A-Za-z0-9._:-]{8,100}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A valid Idempotency-Key is required");
        }
        if (reason == null || reason.isBlank() || reason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A refund reason (1-500 chars) is required");
        }

        Map<String, Object> duplicate = oneOrNull("""
                SELECT r.id,r.payment_id,r.refund_reference,r.amount,r.currency,r.refund_status,
                       r.reason,r.requested_at,r.processed_at
                FROM payment_refunds r WHERE r.idempotency_key=?
                """, idempotencyKey);
        if (duplicate != null) {
            if (number(duplicate.get("payment_id")) != paymentId
                    || ((BigDecimal) duplicate.get("amount")).compareTo(amount) != 0
                    || !currency.equals(duplicate.get("currency"))
                    || !reason.equals(duplicate.get("reason"))) {
                throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Idempotency-Key was already used for a different refund request");
            }
            return camelCase(duplicate);
        }

        Map<String, Object> payment = oneOrNull("""
                SELECT id,order_id,provider,amount,currency,payment_status
                FROM payments WHERE id=? FOR UPDATE
                """, paymentId);
        if (payment == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        String paymentStatus = (String) payment.get("payment_status");
        if (!"PAID".equals(paymentStatus) && !"PARTIALLY_REFUNDED".equals(paymentStatus)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Only settled payments can be refunded");
        }
        if (!currency.equals(payment.get("currency"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund currency does not match payment currency");
        }

        BigDecimal reserved = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM payment_refunds
                WHERE payment_id=? AND refund_status IN ('REQUESTED','PENDING','SUCCEEDED','MANUAL_REVIEW')
                """, BigDecimal.class, paymentId);
        BigDecimal remaining;
        try {
            remaining = RefundAmountPolicy.remaining(
                    (BigDecimal) payment.get("amount"), reserved == null ? BigDecimal.ZERO : reserved);
            RefundAmountPolicy.requireWithinRemaining(amount, remaining);
        } catch (IllegalArgumentException invalidRefund) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, invalidRefund.getMessage());
        }

        String refundReference = "RFD-" + UUID.randomUUID();
        jdbc.update("""
                INSERT INTO payment_refunds
                    (payment_id,refund_reference,idempotency_key,amount,currency,refund_status,
                     reason,requested_by,requested_at)
                VALUES(?,?,?,?,?,'MANUAL_REVIEW',?,?,CURRENT_TIMESTAMP)
                """, paymentId, refundReference, idempotencyKey, amount, currency, reason, staffId);
        Long refundId = jdbc.queryForObject(
                "SELECT id FROM payment_refunds WHERE idempotency_key=?", Long.class, idempotencyKey);
        jdbc.update("""
                INSERT INTO payment_events
                    (payment_id,provider,event_type,event_idempotency_key,
                     verification_status,processing_note,processed_at)
                VALUES(?,?, 'REFUND_REQUESTED',?,'RECEIVED',?,CURRENT_TIMESTAMP)
                """, paymentId, payment.get("provider"), "REFUND-REQUESTED-" + refundId,
                "Refund queued for manual reconciliation; no provider refund is claimed");
        return detail(refundId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> confirmRefund(long refundId, long staffId, String reconciliationReference) {
        if (reconciliationReference == null || !reconciliationReference.matches("[A-Za-z0-9._:-]{6,150}")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A valid reconciliation reference (6-150 safe characters) is required");
        }
        Map<String, Object> refund = oneOrNull("""
                SELECT r.id,r.payment_id,r.amount,r.currency,r.refund_status,
                       p.order_id,p.provider,p.amount AS payment_amount
                FROM payment_refunds r
                JOIN payments p ON p.id=r.payment_id
                WHERE r.id=? FOR UPDATE
                """, refundId);
        if (refund == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund not found");
        if ("SUCCEEDED".equals(refund.get("refund_status"))) return detail(refundId);
        if (!"MANUAL_REVIEW".equals(refund.get("refund_status"))
                && !"PENDING".equals(refund.get("refund_status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund is not awaiting reconciliation");
        }

        jdbc.update("""
                UPDATE payment_refunds SET refund_status='SUCCEEDED',
                    reconciliation_reference=?,processed_at=CURRENT_TIMESTAMP
                WHERE id=? AND refund_status IN ('MANUAL_REVIEW','PENDING')
                """, reconciliationReference, refundId);
        BigDecimal completed = jdbc.queryForObject("""
                SELECT COALESCE(SUM(amount),0) FROM payment_refunds
                WHERE payment_id=? AND refund_status='SUCCEEDED'
                """, BigDecimal.class, number(refund.get("payment_id")));
        String nextStatus = completed.compareTo((BigDecimal) refund.get("payment_amount")) >= 0
                ? "REFUNDED" : "PARTIALLY_REFUNDED";
        int paymentUpdated = jdbc.update(
                "UPDATE payments SET payment_status=? WHERE id=? AND payment_status IN ('PAID','PARTIALLY_REFUNDED')",
                nextStatus, number(refund.get("payment_id")));
        int orderUpdated = jdbc.update(
                "UPDATE customer_orders SET payment_status=? WHERE id=? AND payment_status IN ('PAID','PARTIALLY_REFUNDED')",
                nextStatus, number(refund.get("order_id")));
        if (paymentUpdated != 1 || orderUpdated != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Payment/order state mismatch; refund reconciliation was rolled back");
        }
        jdbc.update("""
                INSERT INTO payment_events
                    (payment_id,provider,event_type,event_idempotency_key,
                     verification_status,processing_note,processed_at)
                VALUES(?,?, 'REFUND_RECONCILED',?,'VERIFIED',?,CURRENT_TIMESTAMP)
                """, number(refund.get("payment_id")), refund.get("provider"),
                "REFUND-RECONCILED-" + refundId,
                "Manually reconciled by staff user " + staffId + "; reference " + reconciliationReference);
        return detail(refundId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> rejectRefund(long refundId, long staffId, String rejectionReason) {
        if (rejectionReason == null || rejectionReason.isBlank() || rejectionReason.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A rejection reason is required");
        }
        Map<String, Object> refund = oneOrNull("""
                SELECT r.id,r.payment_id,r.refund_status,p.provider
                FROM payment_refunds r JOIN payments p ON p.id=r.payment_id
                WHERE r.id=? FOR UPDATE
                """, refundId);
        if (refund == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund not found");
        if ("FAILED".equals(refund.get("refund_status"))) return detail(refundId);
        if (!"MANUAL_REVIEW".equals(refund.get("refund_status"))
                && !"PENDING".equals(refund.get("refund_status"))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Refund is not awaiting review");
        }
        jdbc.update("""
                UPDATE payment_refunds SET refund_status='FAILED',processed_at=CURRENT_TIMESTAMP
                WHERE id=?
                """, refundId);
        jdbc.update("""
                INSERT INTO payment_events
                    (payment_id,provider,event_type,event_idempotency_key,
                     verification_status,processing_note,processed_at)
                VALUES(?,?, 'REFUND_REJECTED',?,'VERIFIED',?,CURRENT_TIMESTAMP)
                """, number(refund.get("payment_id")), refund.get("provider"),
                "REFUND-REJECTED-" + refundId,
                "Rejected by staff user " + staffId + ": " + rejectionReason);
        return detail(refundId);
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> history(long paymentId) {
        if (jdbc.queryForList("SELECT id FROM payments WHERE id=?", paymentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        }
        return jdbc.queryForList("""
                SELECT id,payment_id,refund_reference,provider_refund_id,amount,currency,
                       refund_status,reason,requested_by,requested_at,processed_at
                FROM payment_refunds WHERE payment_id=? ORDER BY requested_at DESC,id DESC
                """, paymentId).stream().map(PaymentRefundService::camelCase).toList();
    }

    private Map<String, Object> detail(long refundId) {
        Map<String, Object> row = oneOrNull("""
                SELECT id,payment_id,refund_reference,provider_refund_id,reconciliation_reference,idempotency_key,
                       amount,currency,refund_status,reason,requested_by,requested_at,processed_at
                FROM payment_refunds WHERE id=?
                """, refundId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Refund not found");
        return camelCase(row);
    }

    private Map<String, Object> oneOrNull(String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        return rows.isEmpty() ? null : rows.getFirst();
    }

    private static void validateAmount(BigDecimal amount) {
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Refund amount must be positive with at most 2 decimals");
        }
    }

    private static long number(Object value) { return ((Number) value).longValue(); }

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
}

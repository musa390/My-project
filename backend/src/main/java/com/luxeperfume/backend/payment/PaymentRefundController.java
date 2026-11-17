package com.luxeperfume.backend.payment;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
public class PaymentRefundController {
    private final PaymentRefundService refunds;

    public PaymentRefundController(PaymentRefundService refunds) {
        this.refunds = refunds;
    }

    @PostMapping("/api/v1/staff/payments/{paymentId}/refunds")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.refund')")
    public ApiResponse<Map<String, Object>> request(Authentication auth,
            @PathVariable long paymentId,
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @RequestBody RefundRequest body) {
        return ApiResponse.success("Refund request queued for manual review",
                refunds.requestRefund(paymentId, staffId(auth), body.amount(),
                        body.currency(), body.reason(), idempotencyKey));
    }

    @GetMapping("/api/v1/staff/payments/{paymentId}/refunds")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.read')")
    public ApiResponse<List<Map<String, Object>>> history(@PathVariable long paymentId) {
        return ApiResponse.success("Refund history retrieved", refunds.history(paymentId));
    }

    @PostMapping("/api/v1/staff/refunds/{refundId}/reconcile")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.refund')")
    public ApiResponse<Map<String, Object>> reconcile(Authentication auth,
            @PathVariable long refundId, @RequestBody ReconcileRequest body) {
        return ApiResponse.success("Refund reconciliation recorded",
                refunds.confirmRefund(refundId, staffId(auth), body.reconciliationReference()));
    }

    @PostMapping("/api/v1/staff/refunds/{refundId}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.refund')")
    public ApiResponse<Map<String, Object>> reject(Authentication auth,
            @PathVariable long refundId, @RequestBody RejectRequest body) {
        return ApiResponse.success("Refund request rejected",
                refunds.rejectRefund(refundId, staffId(auth), body.reason()));
    }

    private static long staffId(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.longValue();
    }

    public record RefundRequest(BigDecimal amount, String currency, String reason) {}
    public record ReconcileRequest(String reconciliationReference) {}
    public record RejectRequest(String reason) {}
}

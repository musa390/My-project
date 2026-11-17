package com.luxeperfume.backend.payment;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
public class PaymentController {
    private final PaymentApplicationService payments;

    public PaymentController(PaymentApplicationService payments) {
        this.payments = payments;
    }

    @PostMapping("/api/v1/orders/{orderNumber}/payments/cod")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<Map<String, Object>> requestCod(Authentication authentication,
            @PathVariable String orderNumber,
            @RequestHeader("Idempotency-Key") String idempotencyKey) {
        return ApiResponse.success("COD payment request recorded",
                payments.initiateCod(customerId(authentication), orderNumber, idempotencyKey));
    }

    @GetMapping("/api/v1/orders/{orderNumber}/payments")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<Map<String, Object>>> history(Authentication authentication,
            @PathVariable String orderNumber) {
        return ApiResponse.success("Payment history retrieved",
                payments.customerHistory(customerId(authentication), orderNumber));
    }

    @PostMapping("/api/v1/staff/payments/{paymentId}/cod-collection")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_payments.read') and hasAuthority('PERM_orders.write')")
    public ApiResponse<Map<String, Object>> confirmCodCollection(Authentication authentication,
            @PathVariable long paymentId) {
        return ApiResponse.success("COD collection confirmed",
                payments.confirmCodCollection(paymentId, customerId(authentication)));
    }

    private static long customerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.longValue();
    }
}

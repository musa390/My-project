package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.OrderStatusUpdateRequest;
import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@Validated
@RequestMapping("/api/v1/staff/orders")
@PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
public class StaffOrderController {
    private final StaffOrderApplicationService orders;

    public StaffOrderController(StaffOrderApplicationService orders) {
        this.orders = orders;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_orders.read')")
    public ApiResponse<Map<String, Object>> search(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String paymentStatus,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return ApiResponse.success("Orders retrieved",
                orders.search(status, paymentStatus, q, page, size));
    }

    @GetMapping("/{orderNumber}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_orders.read')")
    public ApiResponse<Map<String, Object>> detail(@PathVariable String orderNumber) {
        return ApiResponse.success("Order details retrieved", orders.detail(orderNumber));
    }

    @PatchMapping("/{orderNumber}/status")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER') and hasAuthority('PERM_orders.write')")
    public ApiResponse<Map<String, Object>> updateStatus(Authentication authentication,
            @PathVariable String orderNumber, @Valid @RequestBody OrderStatusUpdateRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ApiResponse.success("Order status updated",
                orders.updateStatus(orderNumber, principal.longValue(), request));
    }
}

package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.OrderCancellationRequest;
import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerOrderController {
    private final CustomerOrderApplicationService orders;

    public CustomerOrderController(CustomerOrderApplicationService orders) {
        this.orders = orders;
    }

    @GetMapping
    public ApiResponse<Map<String, Object>> history(Authentication authentication,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(50) int size) {
        return ApiResponse.success("Order history retrieved",
                orders.history(customerId(authentication), page, size));
    }

    @GetMapping("/{orderNumber}")
    public ApiResponse<Map<String, Object>> detail(Authentication authentication,
            @PathVariable String orderNumber) {
        return ApiResponse.success("Order details retrieved",
                orders.detail(customerId(authentication), orderNumber));
    }

    @GetMapping("/{orderNumber}/tracking")
    public ApiResponse<Map<String, Object>> tracking(Authentication authentication,
            @PathVariable String orderNumber) {
        return ApiResponse.success("Order tracking retrieved",
                orders.tracking(customerId(authentication), orderNumber));
    }

    @PostMapping("/{orderNumber}/cancel")
    public ApiResponse<Map<String, Object>> cancel(Authentication authentication,
            @PathVariable String orderNumber,
            @Valid @RequestBody(required = false) OrderCancellationRequest request) {
        return ApiResponse.success("Order cancelled",
                orders.cancel(customerId(authentication), orderNumber, request));
    }

    private static long customerId(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return principal.longValue();
    }
}

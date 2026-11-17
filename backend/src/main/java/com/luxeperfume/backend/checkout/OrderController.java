package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.OrderPlacementRequest;
import com.luxeperfume.backend.checkout.dto.OrderPlacementResponse;
import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class OrderController {
    private final OrderApplicationService orders;

    public OrderController(OrderApplicationService orders) {
        this.orders = orders;
    }

    @PostMapping("/api/v1/orders")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<OrderPlacementResponse> placeOrder(Authentication authentication,
                                                            @Valid @RequestBody OrderPlacementRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return ApiResponse.success("Order placed successfully",
                orders.placeOrder(principal.longValue(), request));
    }
}

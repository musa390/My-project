package com.luxeperfume.backend.checkout;

import com.luxeperfume.backend.checkout.dto.CheckoutPreviewRequest;
import com.luxeperfume.backend.checkout.dto.CheckoutQuoteResponse;
import com.luxeperfume.backend.common.dto.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class CheckoutController {
    private final CheckoutApplicationService checkout;

    public CheckoutController(CheckoutApplicationService checkout) {
        this.checkout = checkout;
    }

    @PostMapping("/api/v1/checkout/preview")
    public ApiResponse<CheckoutQuoteResponse> preview(Authentication authentication,
                                                        @Valid @RequestBody CheckoutPreviewRequest request) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Number principal))
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        return ApiResponse.success("Checkout preview calculated", checkout.preview(principal.longValue(), request));
    }
}

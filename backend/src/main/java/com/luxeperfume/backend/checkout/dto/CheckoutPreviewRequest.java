package com.luxeperfume.backend.checkout.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record CheckoutPreviewRequest(
        @NotNull @Positive Long addressId,
        @Size(max = 60) String couponCode
) {}

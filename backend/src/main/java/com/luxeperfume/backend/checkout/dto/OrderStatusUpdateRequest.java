package com.luxeperfume.backend.checkout.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record OrderStatusUpdateRequest(
        @NotBlank
        @Pattern(regexp = "CONFIRMED|PROCESSING|SHIPPED|DELIVERED|CANCELLED")
        String status,
        @Size(max = 500) String note
) {}

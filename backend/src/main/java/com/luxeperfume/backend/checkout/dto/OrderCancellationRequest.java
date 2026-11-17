package com.luxeperfume.backend.checkout.dto;

import jakarta.validation.constraints.Size;

public record OrderCancellationRequest(
        @Size(max = 500) String reason
) {}

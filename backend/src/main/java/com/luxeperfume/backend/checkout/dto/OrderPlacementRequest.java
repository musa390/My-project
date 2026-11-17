package com.luxeperfume.backend.checkout.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record OrderPlacementRequest(
        @NotNull @Positive Long addressId,
        @Size(max = 60) String couponCode,
        @Size(max = 1000) String customerNote
) {}

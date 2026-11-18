package com.luxeperfume.backend.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ReturnDecisionPayload(
        @NotBlank @Pattern(regexp = "APPROVED|REJECTED") String decision,
        @Size(max = 500) String note) {}

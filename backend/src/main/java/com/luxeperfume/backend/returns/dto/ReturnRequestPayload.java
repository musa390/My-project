package com.luxeperfume.backend.returns.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReturnRequestPayload(@NotBlank @Size(max = 500) String reason) {}

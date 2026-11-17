package com.luxeperfume.backend.common.controller;

import com.luxeperfume.backend.common.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping
    public ApiResponse<HealthStatus> getHealth() {
        return ApiResponse.success("Backend is running",
                new HealthStatus("luxe-perfume-backend", "UP", Instant.now()));
    }

    public record HealthStatus(String application, String status, Instant checkedAt) {}
}

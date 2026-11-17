package com.luxeperfume.backend.catalog.dto;

public record CategoryResponse(
        Long id,
        String name,
        String slug,
        String description,
        Integer sortOrder,
        Boolean active
) {
}

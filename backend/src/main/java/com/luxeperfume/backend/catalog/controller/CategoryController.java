package com.luxeperfume.backend.catalog.controller;

import java.util.List;

import com.luxeperfume.backend.catalog.dto.CategoryResponse;
import com.luxeperfume.backend.catalog.service.CategoryService;
import com.luxeperfume.backend.common.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getActiveCategories() {
        return ResponseEntity.ok(
                ApiResponse.success("Categories retrieved successfully",
                        categoryService.getActiveCategories()));
    }
}

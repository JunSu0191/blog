package com.study.blog.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public final class CategoryDto {

    private CategoryDto() {
    }

    public record CategorySummary(
            Long id,
            String name,
            String slug
    ) {
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 220) String slug
    ) {
    }
}


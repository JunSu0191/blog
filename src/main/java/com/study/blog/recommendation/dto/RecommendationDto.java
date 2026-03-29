package com.study.blog.recommendation.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class RecommendationDto {

    private RecommendationDto() {
    }

    public record UpsertRequest(
            @NotNull Long postId,
            @NotNull @Min(1) Integer slot
    ) {
    }

    public record PostSummary(
            Long id,
            String title,
            String subtitle,
            String thumbnailUrl,
            String slug
    ) {
    }

    public record Response(
            Long id,
            Integer slot,
            PostSummary post
    ) {
    }
}

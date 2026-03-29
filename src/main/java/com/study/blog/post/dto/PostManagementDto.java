package com.study.blog.post.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.post.PostStatus;
import com.study.blog.post.PostVisibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class PostManagementDto {

    private PostManagementDto() {
    }

    public record SeriesRef(
            Long id,
            String title,
            String slug,
            Integer order
    ) {
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 255) String subtitle,
            Long categoryId,
            List<Long> tagIds,
            List<String> tags,
            @Size(max = 500) String thumbnailUrl,
            @NotNull JsonNode contentJson,
            @Size(max = 220) String slug,
            @Size(max = 255) String metaTitle,
            @Size(max = 500) String metaDescription,
            PostVisibility visibility,
            PostStatus status,
            LocalDateTime publishedAt,
            LocalDateTime scheduledAt,
            Long seriesId,
            Integer seriesOrder
    ) {
    }

    public record ScheduleRequest(
            @NotNull LocalDateTime scheduledAt
    ) {
    }

    public record SlugCheckResponse(
            String slug,
            boolean available
    ) {
    }

    public record PostSummaryResponse(
            Long id,
            String title,
            String subtitle,
            String slug,
            String thumbnailUrl,
            PostStatus status,
            PostVisibility visibility,
            LocalDateTime publishedAt,
            LocalDateTime scheduledAt,
            ContentDto.CategoryRef category,
            List<ContentDto.TagRef> tags,
            SeriesRef series,
            LocalDateTime updatedAt
    ) {
    }

    public record PostResponse(
            Long id,
            String title,
            String subtitle,
            String excerpt,
            String slug,
            String metaTitle,
            String metaDescription,
            String thumbnailUrl,
            PostStatus status,
            PostVisibility visibility,
            LocalDateTime publishedAt,
            LocalDateTime scheduledAt,
            ContentDto.CategoryRef category,
            List<ContentDto.TagRef> tags,
            ContentDto.AuthorRef author,
            SeriesRef series,
            JsonNode contentJson,
            String contentHtml,
            Integer readTimeMinutes,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

package com.study.blog.series.dto;

import com.study.blog.content.dto.ContentDto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class SeriesDto {

    private SeriesDto() {
    }

    public record UpsertRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 220) String slug,
            @Size(max = 1000) String description,
            @Size(max = 500) String coverImageUrl
    ) {
    }

    public record AssignPostRequest(
            @NotNull Long seriesId,
            Integer order
    ) {
    }

    public record AddPostRequest(
            @NotNull Long postId,
            Integer order
    ) {
    }

    public record UpdateSeriesPostRequest(
            @NotNull Integer order
    ) {
    }

    public record SummaryResponse(
            Long id,
            String title,
            String slug,
            String description,
            String coverImageUrl,
            Long authorId,
            Long postCount,
            ContentDto.AuthorRef author,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record DetailResponse(
            Long id,
            String title,
            String slug,
            String description,
            String coverImageUrl,
            Long authorId,
            Long postCount,
            ContentDto.AuthorRef author,
            List<ContentDto.PostCard> posts,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record AssignmentResponse(
            Long postId,
            Long seriesId,
            Integer order,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }
}

package com.study.blog.post.dto;

import com.fasterxml.jackson.databind.JsonNode;
import com.study.blog.post.PostStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

public final class PostContractDto {

    private PostContractDto() {
    }

    public record PostWriteRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 255) String subtitle,
            Long categoryId,
            List<Long> tagIds,
            List<String> tags,
            @Size(max = 500) String thumbnailUrl,
            @NotNull JsonNode contentJson,
            Boolean publishNow
    ) {
    }

    public record DraftWriteRequest(
            @NotBlank @Size(max = 200) String title,
            @Size(max = 255) String subtitle,
            Long categoryId,
            @Size(max = 500) String thumbnailUrl,
            @NotNull JsonNode contentJson
    ) {
    }

    public record TagSummary(
            Long id,
            String name,
            String slug
    ) {
    }

    public record CategorySummary(
            Long id,
            String name
    ) {
    }

    public record AuthorSummary(
            Long id,
            String username,
            String name,
            String nickname,
            String profileImageUrl
    ) {
    }

    public record TocItem(
            String id,
            String text,
            int level
    ) {
    }

    public record PostListItem(
            Long id,
            String title,
            String subtitle,
            String excerpt,
            String thumbnailUrl,
            Long categoryId,
            String categoryName,
            CategorySummary category,
            Long authorId,
            String authorName,
            List<TagSummary> tags,
            Long viewCount,
            Long likeCount,
            Integer readTimeMinutes,
            LocalDateTime publishedAt,
            AuthorSummary author,
            List<String> imageUrls
    ) {
    }

    public record PostDetailResponse(
            Long id,
            String title,
            String subtitle,
            String excerpt,
            String thumbnailUrl,
            Long categoryId,
            String categoryName,
            CategorySummary category,
            Long authorId,
            String authorName,
            AuthorSummary author,
            PostStatus status,
            JsonNode contentJson,
            String contentHtml,
            List<TocItem> toc,
            List<TagSummary> tags,
            Long viewCount,
            Long likeCount,
            Integer readTimeMinutes,
            LocalDateTime publishedAt,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
    }

    public record RelatedPostResponse(
            Long id,
            String title,
            String thumbnailUrl,
            String excerpt,
            LocalDateTime publishedAt,
            Long viewCount,
            Long likeCount,
            Integer readTimeMinutes,
            List<TagSummary> tags
    ) {
    }

    public record DraftResponse(
            Long id,
            Long authorId,
            String title,
            String subtitle,
            Long categoryId,
            CategorySummary category,
            String thumbnailUrl,
            JsonNode contentJson,
            String contentHtml,
            LocalDateTime autosavedAt,
            LocalDateTime updatedAt,
            LocalDateTime createdAt
    ) {
    }

    public record ImageUploadResponse(
            String url,
            Integer width,
            Integer height,
            Long size
    ) {
    }
}

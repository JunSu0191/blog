package com.study.blog.content.dto;

import com.study.blog.core.response.PageResponse;

import java.time.LocalDateTime;
import java.util.List;

public final class ContentDto {

    private ContentDto() {
    }

    public record CategoryRef(
            Long id,
            String name,
            String slug
    ) {
    }

    public record TagRef(
            Long id,
            String name,
            String slug
    ) {
    }

    public record AuthorRef(
            Long id,
            String username,
            String name
    ) {
    }

    public record FeaturedPost(
            Long id,
            String title,
            String subtitle,
            String thumbnailUrl,
            String slug
    ) {
    }

    public record PostCard(
            Long id,
            String title,
            String subtitle,
            String excerpt,
            String slug,
            String thumbnailUrl,
            CategoryRef category,
            List<TagRef> tags,
            AuthorRef author,
            LocalDateTime publishedAt,
            Integer readTimeMinutes,
            Long viewCount,
            Long likeCount
    ) {
    }

    public record TagListItem(
            Long id,
            String name,
            String slug,
            String description,
            Long postCount
    ) {
    }

    public record CategoryListItem(
            Long id,
            String name,
            String slug,
            String description,
            Long postCount
    ) {
    }

    public record TagHubResponse(
            Long id,
            String name,
            String slug,
            String description,
            Long postCount,
            List<TagRef> relatedTags,
            FeaturedPost featuredPost
    ) {
    }

    public record CategoryHubResponse(
            Long id,
            String name,
            String slug,
            String description,
            Long postCount,
            List<TagRef> relatedTags,
            FeaturedPost featuredPost
    ) {
    }

    public record SearchResponse(
            PageResponse<PostCard> posts,
            List<TagListItem> tags,
            List<AuthorRef> authors,
            List<CategoryListItem> categories
    ) {
    }
}

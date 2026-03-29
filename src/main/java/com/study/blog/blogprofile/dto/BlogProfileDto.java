package com.study.blog.blogprofile.dto;

import com.study.blog.core.response.PageResponse;
import com.study.blog.post.dto.PostContractDto;

import java.time.LocalDateTime;

public final class BlogProfileDto {

    private BlogProfileDto() {
    }

    public record UserSummary(
            Long userId,
            String username,
            String name,
            String nickname,
            LocalDateTime joinedAt
    ) {
    }

    public record ProfileSummary(
            String displayName,
            String bio,
            String avatarUrl,
            String websiteUrl,
            String location
    ) {
    }

    public record StatsSummary(
            Long publishedPostCount
    ) {
    }

    public record PublicProfileResponse(
            String blogPath,
            String blogUrl,
            UserSummary user,
            ProfileSummary profile,
            StatsSummary stats,
            BlogSettingsDto.Response blogSettings,
            PageResponse<PostContractDto.PostListItem> posts
    ) {
    }
}

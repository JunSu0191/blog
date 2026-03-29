package com.study.blog.bookmark.dto;

import java.time.LocalDateTime;

public final class BookmarkDto {

    private BookmarkDto() {
    }

    public record BookmarkStatusResponse(
            Long postId,
            boolean bookmarked,
            LocalDateTime bookmarkedAt
    ) {
    }
}

package com.study.blog.post;

import java.util.Locale;

public enum PostStatus {
    DRAFT,
    SCHEDULED,
    PUBLISHED;

    public static PostStatus fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return DRAFT;
        }
        return PostStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}

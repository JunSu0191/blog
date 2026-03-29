package com.study.blog.post;

import java.util.Locale;

public enum PostSortType {
    LATEST,
    POPULAR,
    VIEWS;

    public static PostSortType fromNullable(String raw) {
        if (raw == null || raw.isBlank()) {
            return LATEST;
        }
        return PostSortType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}

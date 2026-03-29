package com.study.blog.category;

public enum CategoryErrorCode {
    CATEGORY_NOT_FOUND,
    CATEGORY_DUPLICATE_NAME,
    CATEGORY_DUPLICATE_SLUG,
    CATEGORY_IN_USE,
    INVALID_CATEGORY_ID;

    public String code() {
        return name();
    }
}


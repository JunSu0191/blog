package com.study.blog.post;

public enum PostErrorCode {
    INVALID_CONTENT("invalid_content"),
    UNAUTHORIZED_POST_ACCESS("unauthorized_post_access"),
    POST_NOT_FOUND("post_not_found"),
    SLUG_CONFLICT("slug_conflict"),
    UPLOAD_FAILED("upload_failed"),
    GENERIC_SERVER_ERROR("generic_server_error");

    private final String code;

    PostErrorCode(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }
}

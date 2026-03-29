package com.study.blog.oauth;

public class OAuth2LoginException extends RuntimeException {

    private final String errorCode;

    public OAuth2LoginException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public OAuth2LoginException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}

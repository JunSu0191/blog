package com.study.blog.oauth;

import java.util.Locale;

public enum OAuthProvider {
    GOOGLE,
    KAKAO,
    NAVER;

    public static OAuthProvider fromRegistrationId(String registrationId) {
        if (registrationId == null || registrationId.isBlank()) {
            throw new OAuth2LoginException("invalid_provider", "OAuth 제공자 정보가 없습니다.");
        }
        try {
            return OAuthProvider.valueOf(registrationId.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new OAuth2LoginException("unsupported_provider", "지원하지 않는 OAuth 제공자입니다: " + registrationId);
        }
    }
}

package com.study.blog.oauth;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OAuthUserInfoExtractor {

    public OAuthUserInfo extract(OAuthProvider provider, Map<String, Object> attributes) {
        if (attributes == null) {
            throw new OAuth2LoginException("invalid_oauth_attributes", "OAuth 사용자 속성이 비어 있습니다.");
        }

        return switch (provider) {
            case GOOGLE -> extractGoogle(attributes);
            case KAKAO -> extractKakao(attributes);
            case NAVER -> extractNaver(attributes);
        };
    }

    private OAuthUserInfo extractGoogle(Map<String, Object> attributes) {
        String providerUserId = requireNonBlank(asString(attributes.get("sub")),
                "google_missing_sub", "Google 사용자 식별자(sub)가 없습니다.");
        String email = normalizeNullable(asString(attributes.get("email")));
        String name = normalizeNullable(asString(attributes.get("name")));
        return new OAuthUserInfo(OAuthProvider.GOOGLE, providerUserId, email, name);
    }

    private OAuthUserInfo extractKakao(Map<String, Object> attributes) {
        String providerUserId = requireNonBlank(asString(attributes.get("id")),
                "kakao_missing_id", "Kakao 사용자 식별자(id)가 없습니다.");

        Map<String, Object> kakaoAccount = asMap(attributes.get("kakao_account"));
        String email = normalizeNullable(asString(kakaoAccount.get("email")));

        Map<String, Object> profile = asMap(kakaoAccount.get("profile"));
        String name = normalizeNullable(asString(profile.get("nickname")));

        return new OAuthUserInfo(OAuthProvider.KAKAO, providerUserId, email, name);
    }

    private OAuthUserInfo extractNaver(Map<String, Object> attributes) {
        Map<String, Object> response = asMap(attributes.get("response"));
        String providerUserId = requireNonBlank(asString(response.get("id")),
                "naver_missing_id", "Naver 사용자 식별자(id)가 없습니다.");
        String email = normalizeNullable(asString(response.get("email")));

        String name = normalizeNullable(asString(response.get("name")));
        if (name == null) {
            name = normalizeNullable(asString(response.get("nickname")));
        }

        return new OAuthUserInfo(OAuthProvider.NAVER, providerUserId, email, name);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> asMap(Object source) {
        if (source == null) {
            return Map.of();
        }
        if (source instanceof Map<?, ?> map) {
            Map<String, Object> copied = new LinkedHashMap<>();
            map.forEach((key, value) -> {
                if (key != null) {
                    copied.put(String.valueOf(key), value);
                }
            });
            return copied;
        }
        return Map.of();
    }

    private String asString(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String s) {
            return s;
        }
        return String.valueOf(value);
    }

    private String normalizeNullable(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String requireNonBlank(String value, String errorCode, String errorMessage) {
        String normalized = normalizeNullable(value);
        if (normalized == null) {
            throw new OAuth2LoginException(errorCode, errorMessage);
        }
        return normalized;
    }
}

package com.study.blog.oauth;

public record OAuthUserInfo(
        OAuthProvider provider,
        String providerUserId,
        String email,
        String name) {
}

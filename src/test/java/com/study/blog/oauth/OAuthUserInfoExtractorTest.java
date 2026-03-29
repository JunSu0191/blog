package com.study.blog.oauth;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthUserInfoExtractorTest {

    private OAuthUserInfoExtractor extractor;

    @BeforeEach
    void setUp() {
        extractor = new OAuthUserInfoExtractor();
    }

    @Test
    void shouldExtractGoogleAttributes() {
        OAuthUserInfo info = extractor.extract(OAuthProvider.GOOGLE, Map.of(
                "sub", "google-123",
                "email", "user@gmail.com",
                "name", "Google User"));

        assertThat(info.provider()).isEqualTo(OAuthProvider.GOOGLE);
        assertThat(info.providerUserId()).isEqualTo("google-123");
        assertThat(info.email()).isEqualTo("user@gmail.com");
        assertThat(info.name()).isEqualTo("Google User");
    }

    @Test
    void shouldExtractKakaoAttributes() {
        OAuthUserInfo info = extractor.extract(OAuthProvider.KAKAO, Map.of(
                "id", 12345L,
                "kakao_account", Map.of(
                        "email", "user@kakao.com",
                        "profile", Map.of("nickname", "kakao-user"))));

        assertThat(info.provider()).isEqualTo(OAuthProvider.KAKAO);
        assertThat(info.providerUserId()).isEqualTo("12345");
        assertThat(info.email()).isEqualTo("user@kakao.com");
        assertThat(info.name()).isEqualTo("kakao-user");
    }

    @Test
    void shouldExtractNaverWithNicknameFallback() {
        OAuthUserInfo info = extractor.extract(OAuthProvider.NAVER, Map.of(
                "response", Map.of(
                        "id", "naver-abc",
                        "email", "user@naver.com",
                        "nickname", "naver-user")));

        assertThat(info.provider()).isEqualTo(OAuthProvider.NAVER);
        assertThat(info.providerUserId()).isEqualTo("naver-abc");
        assertThat(info.email()).isEqualTo("user@naver.com");
        assertThat(info.name()).isEqualTo("naver-user");
    }

    @Test
    void shouldThrowWhenProviderUserIdMissing() {
        assertThatThrownBy(() -> extractor.extract(OAuthProvider.GOOGLE, Map.of("email", "x@gmail.com")))
                .isInstanceOf(OAuth2LoginException.class)
                .hasMessageContaining("식별자");
    }
}

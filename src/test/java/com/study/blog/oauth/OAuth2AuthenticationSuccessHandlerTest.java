package com.study.blog.oauth;

import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OAuth2AuthenticationSuccessHandlerTest {

    @Mock
    private OAuthAccountService oauthAccountService;
    @Mock
    private JwtUtil jwtUtil;

    private OAuth2AuthenticationSuccessHandler handler;

    @BeforeEach
    void setUp() {
        handler = new OAuth2AuthenticationSuccessHandler(
                oauthAccountService,
                jwtUtil,
                "http://localhost:5173/auth/callback",
                "http://localhost:5173/auth/callback");
    }

    @Test
    void shouldRedirectWithTokenOnSuccess() throws Exception {
        OAuth2UserPrincipal principal = oauthPrincipal();
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");

        User user = User.builder()
                .id(1L)
                .username("google_user")
                .password("encoded")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .deletedYn("N")
                .build();

        when(oauthAccountService.loginOrRegister(principal.getUserInfo())).thenReturn(user);
        when(jwtUtil.generateToken("google_user")).thenReturn("jwt-token-value");

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5173/auth/callback#token=jwt-token-value");
    }

    @Test
    void shouldRedirectFailureWhenMappingFails() throws Exception {
        OAuth2UserPrincipal principal = oauthPrincipal();
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                principal,
                principal.getAuthorities(),
                "google");

        when(oauthAccountService.loginOrRegister(principal.getUserInfo()))
                .thenThrow(new OAuth2LoginException("account_suspended", "정지된 계정입니다."));

        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(new MockHttpServletRequest(), response, authentication);

        assertThat(response.getStatus()).isEqualTo(302);
        assertThat(response.getRedirectedUrl()).contains("error=account_suspended");
        assertThat(response.getRedirectedUrl()).contains("message=%EC%A0%95%EC%A7%80%EB%90%9C");
    }

    private OAuth2UserPrincipal oauthPrincipal() {
        OAuth2User delegate = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("sub", "google-123", "email", "user@gmail.com", "name", "Google User"),
                "sub");
        OAuthUserInfo info = new OAuthUserInfo(OAuthProvider.GOOGLE, "google-123", "user@gmail.com", "Google User");
        return new OAuth2UserPrincipal(delegate, info);
    }
}

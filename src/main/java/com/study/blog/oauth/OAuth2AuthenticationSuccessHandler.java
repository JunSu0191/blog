package com.study.blog.oauth;

import com.study.blog.security.JwtUtil;
import com.study.blog.user.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final OAuthAccountService oauthAccountService;
    private final JwtUtil jwtUtil;
    private final String successRedirectUri;
    private final String failureRedirectUri;

    public OAuth2AuthenticationSuccessHandler(OAuthAccountService oauthAccountService,
                                              JwtUtil jwtUtil,
                                              @Value("${app.oauth2.success-redirect-uri:http://localhost:5173/auth/callback}") String successRedirectUri,
                                              @Value("${app.oauth2.failure-redirect-uri:http://localhost:5173/auth/callback}") String failureRedirectUri) {
        this.oauthAccountService = oauthAccountService;
        this.jwtUtil = jwtUtil;
        this.successRedirectUri = successRedirectUri;
        this.failureRedirectUri = failureRedirectUri;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            if (!(authentication.getPrincipal() instanceof OAuth2UserPrincipal principal)) {
                throw new OAuth2LoginException("invalid_oauth_principal", "OAuth 사용자 정보가 올바르지 않습니다.");
            }

            User user = oauthAccountService.loginOrRegister(principal.getUserInfo());
            String jwt = jwtUtil.generateToken(user.getUsername());

            String redirectUrl = UriComponentsBuilder.fromUriString(successRedirectUri)
                    .fragment("token=" + jwt)
                    .build()
                    .toUriString();
            response.sendRedirect(redirectUrl);
        } catch (OAuth2LoginException ex) {
            redirectFailure(response, ex.getErrorCode(), ex.getMessage());
        } catch (Exception ex) {
            redirectFailure(response, "oauth_login_failed", "소셜 로그인 처리 중 오류가 발생했습니다.");
        }
    }

    private void redirectFailure(HttpServletResponse response, String code, String message) throws IOException {
        String redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("error", code)
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}

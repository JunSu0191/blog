package com.study.blog.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
public class OAuth2AuthenticationFailureHandler implements AuthenticationFailureHandler {

    private final String failureRedirectUri;

    public OAuth2AuthenticationFailureHandler(
            @Value("${app.oauth2.failure-redirect-uri:http://localhost:5173/auth/callback}") String failureRedirectUri) {
        this.failureRedirectUri = failureRedirectUri;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        org.springframework.security.core.AuthenticationException exception)
            throws IOException, ServletException {
        String code = "oauth_authorization_failed";
        String message = "소셜 로그인 인증에 실패했습니다.";

        if (exception instanceof OAuth2AuthenticationException oauth2Ex) {
            String errorCode = oauth2Ex.getError() != null ? oauth2Ex.getError().getErrorCode() : null;
            if (errorCode != null && !errorCode.isBlank()) {
                code = errorCode;
            }
            if (oauth2Ex.getMessage() != null && !oauth2Ex.getMessage().isBlank()) {
                message = oauth2Ex.getMessage();
            }
        } else if (exception.getMessage() != null && !exception.getMessage().isBlank()) {
            message = exception.getMessage();
        }

        String redirectUrl = UriComponentsBuilder.fromUriString(failureRedirectUri)
                .queryParam("error", code)
                .queryParam("message", message)
                .build()
                .encode()
                .toUriString();
        response.sendRedirect(redirectUrl);
    }
}

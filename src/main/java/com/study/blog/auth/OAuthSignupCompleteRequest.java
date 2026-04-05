package com.study.blog.auth;

import jakarta.validation.constraints.NotBlank;

public record OAuthSignupCompleteRequest(
        @NotBlank String signupToken,
        @NotBlank String username,
        @NotBlank String nickname) {
}

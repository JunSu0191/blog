package com.study.blog.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        String name,
        @NotBlank String nickname,
        String email,
        @NotBlank String phoneNumber,
        @NotNull Long verificationId) {
}

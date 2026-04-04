package com.study.blog.auth;

import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(
        @NotBlank String username,
        @NotBlank String password,
        String name,
        @NotBlank String nickname,
        String email,
        String phoneNumber,
        Long verificationId) {
}

package com.study.blog.auth;

import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;

public class AuthResponse {

    public record UserSummary(Long id, String username, String name, String nickname,
                              String email, String phoneNumber,
                              UserRole role, UserStatus status, Boolean mustChangePassword) {
    }

    public record Login(String token, UserSummary user) {
    }
}

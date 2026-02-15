package com.study.blog.auth;

public class AuthResponse {

    public record UserSummary(Long id, String username, String name) {
    }

    public record Login(String token, UserSummary user) {
    }
}

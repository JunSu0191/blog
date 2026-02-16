package com.study.blog.admin.dto;

import com.study.blog.user.UserRole;
import com.study.blog.user.UserStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

public class AdminDto {

    @Data
    public static class MeResponse {
        private Long id;
        private String username;
        private String name;
        private UserRole role;
        private UserStatus status;
        private Boolean mustChangePassword;
        private LocalDateTime createdAt;
    }

    @Data
    public static class DashboardSummaryResponse {
        private Long totalUsers;
        private Long totalPosts;
        private Long totalComments;
        private Long totalConversations;
        private Long totalNotifications;
    }

    @Data
    public static class UserSummaryResponse {
        private Long id;
        private String username;
        private String name;
        private UserRole role;
        private UserStatus status;
        private Boolean mustChangePassword;
        private LocalDateTime createdAt;
    }

    @Data
    public static class UpdateUserRoleRequest {
        @NotNull
        private UserRole role;
    }

    @Data
    public static class UpdateUserStatusRequest {
        @NotNull
        private UserStatus status;
    }

    @Data
    public static class PostSummaryResponse {
        private Long id;
        private Long userId;
        private String username;
        private String title;
        private String deletedYn;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
    }

    @Data
    public static class CommentSummaryResponse {
        private Long id;
        private Long postId;
        private String postTitle;
        private Long userId;
        private String username;
        private String content;
        private String deletedYn;
        private LocalDateTime createdAt;
        private LocalDateTime deletedAt;
    }
}

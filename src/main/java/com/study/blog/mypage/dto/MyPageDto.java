package com.study.blog.mypage.dto;

import lombok.Data;

public class MyPageDto {

    @Data
    public static class StatsResponse {
        private Long postCount;
        private Long commentCount;
        private Long likedPostCount;
    }

    @Data
    public static class ProfileResponse {
        private String displayName;
        private String bio;
        private String avatarUrl;
        private String websiteUrl;
        private String location;
    }

    @Data
    public static class SummaryResponse {
        private Long userId;
        private String username;
        private String name;
        private ProfileResponse profile;
        private StatsResponse stats;
    }

    @Data
    public static class UpdateProfileRequest {
        private String name;
        private String displayName;
        private String bio;
        private String avatarUrl;
        private String websiteUrl;
        private String location;
    }
}

package com.study.blog.mypage.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

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
        private String nickname;
        private String email;
        private String phoneNumber;
        private ProfileResponse profile;
        private StatsResponse stats;
    }

    @Getter
    @Setter
    public static class UpdateProfileRequest {
        private String name;
        private String nickname;
        private String email;
        private String phoneNumber;
        private String displayName;
        private String bio;
        private String avatarUrl;
        private String websiteUrl;
        private String location;

        @JsonIgnore
        private boolean avatarUrlPresent;

        public void setAvatarUrl(String avatarUrl) {
            this.avatarUrl = avatarUrl;
            this.avatarUrlPresent = true;
        }
    }
}

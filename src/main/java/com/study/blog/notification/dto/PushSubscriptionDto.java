package com.study.blog.notification.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDateTime;

public class PushSubscriptionDto {

    @Data
    public static class SaveRequest {
        @NotBlank
        private String endpoint;

        @Valid
        private Keys keys;

        private String userAgent;
    }

    @Data
    public static class DeleteRequest {
        @NotBlank
        private String endpoint;
    }

    @Data
    public static class Keys {
        @NotBlank
        private String p256dh;

        @NotBlank
        private String auth;
    }

    @Data
    public static class Response {
        private Long id;
        private String endpoint;
        private String userAgent;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private LocalDateTime lastUsedAt;
        private boolean active;
    }

    @Data
    public static class PublicKeyResponse {
        private String publicKey;
    }
}

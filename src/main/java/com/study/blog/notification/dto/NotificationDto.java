package com.study.blog.notification.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

public class NotificationDto {

    @Data
    public static class Response {
        private Long id;
        private Long userId;
        private String type;
        private String title;
        private String body;
        private Map<String, Object> payload;
        private LocalDateTime createdAt;
        private LocalDateTime readAt;
        private LocalDateTime archivedAt;
    }

    @Data
    public static class Event {
        private String type;
        private Response notification;
    }
}

package com.study.blog.attach.dto;

import java.time.LocalDateTime;

public class AttachFileDto {

    public static class Request {
        public Long postId;
        public String originalName;
        public String storedName;
        public String path;
    }

    public static class Response {
        public Long id;
        public Long postId;
        public String originalName;
        public String storedName;
        public String path;
        public String deletedYn;
        public LocalDateTime createdAt;
    }
}

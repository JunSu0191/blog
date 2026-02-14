package com.study.blog.post.dto;

import com.study.blog.attach.dto.AttachFileDto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 게시글 API용 요청/응답 DTO 모음입니다.
 */
public class PostDto {

    // 게시글 생성/수정 요청 바디 (userId는 JWT 토큰에서 자동 추출)
    public static class Request {
        public String title;
        public String content;
    }

    // 클라이언트에 반환할 응답 객체
    public static class Response {
        public Long id;
        public Long userId;
        public String title;
        public String content;
        public String deletedYn;
        public LocalDateTime createdAt;
        public List<AttachFileDto.Response> attachFiles;
    }
}

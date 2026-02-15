package com.study.blog.comment.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 댓글 API용 요청/응답 DTO 모음입니다.
 */
public class CommentDto {

    // 댓글 생성 요청 바디
    public static class CreateRequest {
        public Long postId;
        public Long parentId; // 대댓글용, null이면 일반 댓글
        public String content;
    }

    // 댓글 수정 요청 바디
    public static class UpdateRequest {
        public String content;
    }

    // 클라이언트에 반환할 응답 객체
    public static class Response {
        public Long id;
        public Long postId;
        public Long userId;
        public String name; // 사용자 이름
        public Long parentId; // 부모 댓글 ID
        public String content;
        public String deletedYn;
        public Long likeCount;
        public Long dislikeCount;
        public String myReaction;
        public LocalDateTime createdAt;
        public LocalDateTime updatedAt;
        public List<Response> replies; // 대댓글 목록
    }
}

package com.study.blog.like.dto;

import com.study.blog.like.CommentReactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

public class LikeDto {

    @Data
    public static class PostLikeResponse {
        private Long postId;
        private boolean liked;
        private Long likeCount;
    }

    @Data
    public static class CommentReactionRequest {
        @NotNull
        private CommentReactionType reactionType;
    }

    @Data
    public static class CommentReactionResponse {
        private Long commentId;
        private CommentReactionType myReaction;
        private Long likeCount;
        private Long dislikeCount;
    }
}

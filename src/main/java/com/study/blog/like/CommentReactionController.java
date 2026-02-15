package com.study.blog.like;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.like.dto.LikeDto;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/comments/{commentId}/reaction")
public class CommentReactionController {

    private final CommentReactionService commentReactionService;
    private final CurrentUserResolver currentUserResolver;

    public CommentReactionController(CommentReactionService commentReactionService,
                                     CurrentUserResolver currentUserResolver) {
        this.commentReactionService = commentReactionService;
        this.currentUserResolver = currentUserResolver;
    }

    @PutMapping
    public ResponseEntity<ApiResponseTemplate<LikeDto.CommentReactionResponse>> updateReaction(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long commentId,
            @Valid @RequestBody LikeDto.CommentReactionRequest req) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(commentReactionService.updateReaction(userId, commentId, req.getReactionType()));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseTemplate<LikeDto.CommentReactionResponse>> getMyReaction(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long commentId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(commentReactionService.getMyReaction(userId, commentId));
    }
}

package com.study.blog.like;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.like.dto.LikeDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts/{postId}/likes")
public class PostLikeController {

    private final PostLikeService postLikeService;
    private final CurrentUserResolver currentUserResolver;

    public PostLikeController(PostLikeService postLikeService, CurrentUserResolver currentUserResolver) {
        this.postLikeService = postLikeService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping
    public ResponseEntity<ApiResponseTemplate<LikeDto.PostLikeResponse>> like(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long postId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postLikeService.like(userId, postId));
    }

    @DeleteMapping
    public ResponseEntity<ApiResponseTemplate<LikeDto.PostLikeResponse>> unlike(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long postId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postLikeService.unlike(userId, postId));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponseTemplate<LikeDto.PostLikeResponse>> myLikeStatus(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PathVariable Long postId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postLikeService.getLikeStatus(userId, postId));
    }
}

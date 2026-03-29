package com.study.blog.post;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.PageResponse;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.post.dto.PostManagementDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Post Management")
public class PostManagementController {

    private final PostManagementService postManagementService;
    private final CurrentUserResolver currentUserResolver;

    public PostManagementController(PostManagementService postManagementService,
                                    CurrentUserResolver currentUserResolver) {
        this.postManagementService = postManagementService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me/posts")
    @Operation(summary = "내 게시글 관리 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<PostManagementDto.PostSummaryResponse>>> listMyPosts(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam(required = false) String status,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        Page<PostManagementDto.PostSummaryResponse> page = postManagementService.listMyPosts(userId, status, pageable);
        return ApiResponseFactory.ok(page);
    }

    @PostMapping("/posts")
    @Operation(summary = "게시글 생성")
    public ResponseEntity<ApiResponseTemplate<PostManagementDto.PostResponse>> createPost(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostManagementDto.UpsertRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postManagementService.createPost(userId, request));
    }

    @PutMapping("/posts/{postId}")
    @Operation(summary = "게시글 수정")
    public ResponseEntity<ApiResponseTemplate<PostManagementDto.PostResponse>> updatePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostManagementDto.UpsertRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postManagementService.updatePost(userId, postId, request));
    }

    @PostMapping("/posts/{postId}/publish")
    @Operation(summary = "게시글 즉시 발행")
    public ResponseEntity<ApiResponseTemplate<PostManagementDto.PostResponse>> publishPost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postManagementService.publishPost(userId, postId));
    }

    @PostMapping("/posts/{postId}/schedule")
    @Operation(summary = "게시글 예약 발행")
    public ResponseEntity<ApiResponseTemplate<PostManagementDto.PostResponse>> schedulePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostManagementDto.ScheduleRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postManagementService.schedulePost(userId, postId, request));
    }

    @GetMapping("/posts/slug/check")
    @Operation(summary = "게시글 slug 사용 가능 여부 확인")
    public ResponseEntity<ApiResponseTemplate<PostManagementDto.SlugCheckResponse>> checkSlug(@RequestParam String slug) {
        return ApiResponseFactory.ok(postManagementService.checkSlug(slug));
    }
}

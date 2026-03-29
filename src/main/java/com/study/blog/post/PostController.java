package com.study.blog.post;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.post.dto.PostContractDto;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
@Validated
public class PostController {

    private final PostApplicationService postApplicationService;
    private final PostDraftService postDraftService;
    private final CurrentUserResolver currentUserResolver;

    public PostController(PostApplicationService postApplicationService,
                          PostDraftService postDraftService,
                          CurrentUserResolver currentUserResolver) {
        this.postApplicationService = postApplicationService;
        this.postDraftService = postDraftService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/drafts")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.DraftResponse>> createDraft(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostContractDto.DraftWriteRequest request) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        PostContractDto.DraftResponse response = postDraftService.createDraft(request, actorUserId);
        return ApiResponseFactory.created(URI.create("/api/posts/drafts/" + response.id()), response);
    }

    @PutMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.DraftResponse>> updateDraft(
            @PathVariable Long draftId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostContractDto.DraftWriteRequest request) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        PostContractDto.DraftResponse response = postDraftService.updateDraft(draftId, request, actorUserId);
        return ApiResponseFactory.ok(response);
    }

    @GetMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.DraftResponse>> getDraft(
            @PathVariable Long draftId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(postDraftService.getDraft(draftId, actorUserId));
    }

    @GetMapping("/drafts")
    public ResponseEntity<ApiResponseTemplate<com.study.blog.core.response.PageResponse<PostContractDto.DraftResponse>>> listDrafts(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PageableDefault(size = 10) Pageable pageable) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        Page<PostContractDto.DraftResponse> response = postDraftService.listDrafts(actorUserId, pageable);
        return ApiResponseFactory.ok(response);
    }

    @DeleteMapping("/drafts/{draftId}")
    public ResponseEntity<ApiResponseTemplate<Void>> deleteDraft(
            @PathVariable Long draftId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        postDraftService.deleteDraft(draftId, actorUserId);
        return ApiResponseFactory.noContent();
    }

    @PostMapping
    public ResponseEntity<ApiResponseTemplate<PostContractDto.PostDetailResponse>> createPost(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostContractDto.PostWriteRequest request) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        PostContractDto.PostDetailResponse response = postApplicationService.createPost(request, actorUserId);
        return ApiResponseFactory.created(URI.create("/api/posts/" + response.id()), response);
    }

    @PutMapping("/{postId}")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.PostDetailResponse>> updatePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody PostContractDto.PostWriteRequest request) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        PostContractDto.PostDetailResponse response = postApplicationService.updatePost(postId, request, actorUserId);
        return ApiResponseFactory.ok(response);
    }

    @DeleteMapping("/{postId}")
    public ResponseEntity<ApiResponseTemplate<Void>> deletePost(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        postApplicationService.deletePost(postId, actorUserId);
        return ApiResponseFactory.noContent();
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<com.study.blog.core.response.PageResponse<PostContractDto.PostListItem>>> listPosts(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "latest") String sort,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<PostContractDto.PostListItem> response = postApplicationService.listPublishedPosts(
                q,
                categoryId,
                tag,
                sort,
                pageable);
        return ApiResponseFactory.ok(response);
    }

    @GetMapping("/{postId}")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.PostDetailResponse>> getPost(
            @PathVariable Long postId) {
        Long actorUserId = currentUserResolver.resolveFromSecurityContextOrNull();
        return ApiResponseFactory.ok(postApplicationService.getPostById(postId, actorUserId));
    }

    @GetMapping("/{postId}/related")
    public ResponseEntity<ApiResponseTemplate<List<PostContractDto.RelatedPostResponse>>> getRelatedPosts(
            @PathVariable Long postId,
            @RequestParam(defaultValue = "5") int limit) {
        return ApiResponseFactory.ok(postApplicationService.getRelatedPosts(postId, limit));
    }
}

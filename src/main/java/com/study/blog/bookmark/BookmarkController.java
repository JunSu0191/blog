package com.study.blog.bookmark;

import com.study.blog.bookmark.dto.BookmarkDto;
import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.PageResponse;
import com.study.blog.core.security.CurrentUserResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Bookmarks")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final CurrentUserResolver currentUserResolver;

    public BookmarkController(BookmarkService bookmarkService, CurrentUserResolver currentUserResolver) {
        this.bookmarkService = bookmarkService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/me/bookmarks")
    @Operation(summary = "내 저장 글 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ContentDto.PostCard>>> listBookmarks(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @PageableDefault(size = 20) Pageable pageable) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        Page<ContentDto.PostCard> page = bookmarkService.listBookmarks(userId, pageable);
        return ApiResponseFactory.ok(page);
    }

    @PostMapping("/posts/{postId}/bookmark")
    @Operation(summary = "게시글 저장")
    public ResponseEntity<ApiResponseTemplate<BookmarkDto.BookmarkStatusResponse>> bookmark(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(bookmarkService.bookmark(userId, postId));
    }

    @DeleteMapping("/posts/{postId}/bookmark")
    @Operation(summary = "게시글 저장 해제")
    public ResponseEntity<ApiResponseTemplate<Void>> unbookmark(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        bookmarkService.unbookmark(userId, postId);
        return ApiResponseFactory.noContent();
    }

    @GetMapping("/posts/{postId}/bookmark-status")
    @Operation(summary = "게시글 저장 상태 조회")
    public ResponseEntity<ApiResponseTemplate<BookmarkDto.BookmarkStatusResponse>> getBookmarkStatus(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(bookmarkService.getStatus(userId, postId));
    }
}

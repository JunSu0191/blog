package com.study.blog.content;

import com.study.blog.content.dto.ContentDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Content Discovery")
public class ContentDiscoveryController {

    private final ContentDiscoveryService contentDiscoveryService;

    public ContentDiscoveryController(ContentDiscoveryService contentDiscoveryService) {
        this.contentDiscoveryService = contentDiscoveryService;
    }

    @GetMapping("/tags")
    @Operation(summary = "태그 허브 목록 조회")
    public ResponseEntity<ApiResponseTemplate<List<ContentDto.TagListItem>>> listTags() {
        return ApiResponseFactory.ok(contentDiscoveryService.listTags());
    }

    @GetMapping("/tags/{slug}")
    @Operation(summary = "태그 허브 상세 조회")
    public ResponseEntity<ApiResponseTemplate<ContentDto.TagHubResponse>> getTagHub(@PathVariable String slug) {
        return ApiResponseFactory.ok(contentDiscoveryService.getTagHub(slug));
    }

    @GetMapping("/tags/{slug}/posts")
    @Operation(summary = "태그별 게시글 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ContentDto.PostCard>>> listTagPosts(
            @PathVariable String slug,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ContentDto.PostCard> page = contentDiscoveryService.listTagPosts(slug, pageable);
        return ApiResponseFactory.ok(page);
    }

    @GetMapping("/categories/{categoryId}")
    @Operation(summary = "카테고리 허브 상세 조회")
    public ResponseEntity<ApiResponseTemplate<ContentDto.CategoryHubResponse>> getCategoryHub(@PathVariable Long categoryId) {
        return ApiResponseFactory.ok(contentDiscoveryService.getCategoryHub(categoryId));
    }

    @GetMapping("/categories/{categoryId}/posts")
    @Operation(summary = "카테고리별 게시글 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ContentDto.PostCard>>> listCategoryPosts(
            @PathVariable Long categoryId,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ContentDto.PostCard> page = contentDiscoveryService.listCategoryPosts(categoryId, pageable);
        return ApiResponseFactory.ok(page);
    }

    @GetMapping("/search")
    @Operation(summary = "통합 검색")
    public ResponseEntity<ApiResponseTemplate<ContentDto.SearchResponse>> search(
            @RequestParam("q") String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponseFactory.ok(contentDiscoveryService.search(keyword, pageable));
    }

    @GetMapping("/search/trending")
    @Operation(summary = "검색 트렌딩 데이터 조회")
    public ResponseEntity<ApiResponseTemplate<ContentDto.SearchResponse>> trending() {
        return ApiResponseFactory.ok(contentDiscoveryService.trending());
    }

    @GetMapping("/search/recent")
    @Operation(summary = "검색 최신 데이터 조회")
    public ResponseEntity<ApiResponseTemplate<ContentDto.SearchResponse>> recent() {
        return ApiResponseFactory.ok(contentDiscoveryService.recent());
    }
}

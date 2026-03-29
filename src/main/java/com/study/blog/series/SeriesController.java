package com.study.blog.series;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.PageResponse;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.series.dto.SeriesDto;
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
@Tag(name = "Series")
public class SeriesController {

    private final SeriesService seriesService;
    private final CurrentUserResolver currentUserResolver;

    public SeriesController(SeriesService seriesService, CurrentUserResolver currentUserResolver) {
        this.seriesService = seriesService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping("/series")
    @Operation(summary = "시리즈 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<SeriesDto.SummaryResponse>>> listSeries(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<SeriesDto.SummaryResponse> page = seriesService.listSeries(pageable);
        return ApiResponseFactory.ok(page);
    }

    @GetMapping("/series/{seriesId}")
    @Operation(summary = "시리즈 상세 조회")
    public ResponseEntity<ApiResponseTemplate<SeriesDto.DetailResponse>> getSeries(@PathVariable Long seriesId) {
        return ApiResponseFactory.ok(seriesService.getSeries(seriesId));
    }

    @PostMapping("/series")
    @Operation(summary = "시리즈 생성")
    public ResponseEntity<ApiResponseTemplate<SeriesDto.DetailResponse>> createSeries(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody SeriesDto.UpsertRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(seriesService.createSeries(userId, request));
    }

    @PutMapping("/series/{seriesId}")
    @Operation(summary = "시리즈 수정")
    public ResponseEntity<ApiResponseTemplate<SeriesDto.DetailResponse>> updateSeries(
            @PathVariable Long seriesId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody SeriesDto.UpsertRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(seriesService.updateSeries(seriesId, userId, request));
    }

    @PostMapping("/posts/{postId}/series")
    @Operation(summary = "게시글 시리즈 지정")
    public ResponseEntity<ApiResponseTemplate<SeriesDto.AssignmentResponse>> assignPostToSeries(
            @PathVariable Long postId,
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody SeriesDto.AssignPostRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(seriesService.assignPost(userId, postId, request));
    }
}

package com.study.blog.report;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.response.PageResponse;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.report.dto.ReportDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@Tag(name = "Reports")
public class ReportController {

    private final ReportService reportService;
    private final CurrentUserResolver currentUserResolver;

    public ReportController(ReportService reportService, CurrentUserResolver currentUserResolver) {
        this.reportService = reportService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/reports")
    @Operation(summary = "콘텐츠 신고 접수")
    public ResponseEntity<ApiResponseTemplate<ReportDto.Response>> createReport(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody ReportDto.CreateRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(reportService.createReport(userId, request));
    }

    @GetMapping("/admin/reports")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 신고 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ReportDto.Response>>> listReports(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportDto.Response> page = reportService.listReports(pageable);
        return ApiResponseFactory.ok(page);
    }

    @PostMapping("/admin/reports/{reportId}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 신고 처리")
    public ResponseEntity<ApiResponseTemplate<ReportDto.Response>> resolveReport(
            Authentication authentication,
            @PathVariable Long reportId,
            @Valid @RequestBody ReportDto.ResolveRequest request) {
        return ApiResponseFactory.ok(reportService.resolveReport(authentication.getName(), reportId, request));
    }

    @GetMapping("/admin/moderation/posts")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 게시글 검수 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ReportDto.ModerationPostResponse>>> listModerationPosts(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportDto.ModerationPostResponse> page = reportService.listModerationPosts(pageable);
        return ApiResponseFactory.ok(page);
    }

    @GetMapping("/admin/moderation/comments")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자 댓글 검수 목록 조회")
    public ResponseEntity<ApiResponseTemplate<PageResponse<ReportDto.ModerationCommentResponse>>> listModerationComments(
            @PageableDefault(size = 20) Pageable pageable) {
        Page<ReportDto.ModerationCommentResponse> page = reportService.listModerationComments(pageable);
        return ApiResponseFactory.ok(page);
    }
}

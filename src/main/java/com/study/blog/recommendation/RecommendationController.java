package com.study.blog.recommendation;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.recommendation.dto.RecommendationDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/recommendations")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Recommendations")
public class RecommendationController {

    private final RecommendationService recommendationService;

    public RecommendationController(RecommendationService recommendationService) {
        this.recommendationService = recommendationService;
    }

    @GetMapping
    @Operation(summary = "추천 슬롯 목록 조회")
    public ResponseEntity<ApiResponseTemplate<List<RecommendationDto.Response>>> listRecommendations() {
        return ApiResponseFactory.ok(recommendationService.listRecommendations());
    }

    @PostMapping
    @Operation(summary = "추천 슬롯 등록 또는 교체")
    public ResponseEntity<ApiResponseTemplate<RecommendationDto.Response>> upsertRecommendation(
            Authentication authentication,
            @Valid @RequestBody RecommendationDto.UpsertRequest request) {
        return ApiResponseFactory.ok(recommendationService.upsertRecommendation(authentication.getName(), request));
    }

    @DeleteMapping("/{recommendationId}")
    @Operation(summary = "추천 슬롯 삭제")
    public ResponseEntity<ApiResponseTemplate<Void>> deleteRecommendation(@PathVariable Long recommendationId) {
        recommendationService.deleteRecommendation(recommendationId);
        return ApiResponseFactory.noContent();
    }
}

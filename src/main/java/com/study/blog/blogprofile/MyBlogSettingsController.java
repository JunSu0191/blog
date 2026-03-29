package com.study.blog.blogprofile;

import com.study.blog.blogprofile.dto.BlogSettingsDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me/blog/settings")
public class MyBlogSettingsController {

    private final BlogSettingsService blogSettingsService;
    private final CurrentUserResolver currentUserResolver;

    public MyBlogSettingsController(BlogSettingsService blogSettingsService,
                                    CurrentUserResolver currentUserResolver) {
        this.blogSettingsService = blogSettingsService;
        this.currentUserResolver = currentUserResolver;
    }

    @GetMapping
    public ResponseEntity<ApiResponseTemplate<BlogSettingsDto.Response>> getMySettings(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(blogSettingsService.getMySettings(userId));
    }

    @PutMapping
    public ResponseEntity<ApiResponseTemplate<BlogSettingsDto.Response>> updateMySettings(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @Valid @RequestBody BlogSettingsDto.UpdateRequest request) {
        Long userId = currentUserResolver.resolveFromRest(xUserId);
        return ApiResponseFactory.ok(blogSettingsService.upsertMySettings(userId, request));
    }
}

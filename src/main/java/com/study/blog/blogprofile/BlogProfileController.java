package com.study.blog.blogprofile;

import com.study.blog.blogprofile.dto.BlogProfileDto;
import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/blogs")
@Validated
public class BlogProfileController {

    private final BlogProfileService blogProfileService;

    public BlogProfileController(BlogProfileService blogProfileService) {
        this.blogProfileService = blogProfileService;
    }

    @GetMapping("/{username}")
    public ResponseEntity<ApiResponseTemplate<BlogProfileDto.PublicProfileResponse>> getPublicProfile(
            @PathVariable String username,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "latest") String sort,
            @PageableDefault(size = 10) Pageable pageable) {
        return ApiResponseFactory.ok(blogProfileService.getPublicProfile(username, q, sort, pageable));
    }
}

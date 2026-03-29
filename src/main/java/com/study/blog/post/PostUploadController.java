package com.study.blog.post;

import com.study.blog.core.response.ApiResponseFactory;
import com.study.blog.core.response.ApiResponseTemplate;
import com.study.blog.core.security.CurrentUserResolver;
import com.study.blog.post.dto.PostContractDto;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@RequestMapping("/api/uploads")
@Validated
public class PostUploadController {

    private final PostImageUploadService postImageUploadService;
    private final CurrentUserResolver currentUserResolver;

    public PostUploadController(PostImageUploadService postImageUploadService,
                                CurrentUserResolver currentUserResolver) {
        this.postImageUploadService = postImageUploadService;
        this.currentUserResolver = currentUserResolver;
    }

    @PostMapping("/images")
    public ResponseEntity<ApiResponseTemplate<PostContractDto.ImageUploadResponse>> uploadImage(
            @RequestHeader(value = "X-User-Id", required = false) Long xUserId,
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        Long actorUserId = currentUserResolver.resolveFromRest(xUserId);
        PostContractDto.ImageUploadResponse response = postImageUploadService.uploadImage(file, actorUserId, request);
        return ApiResponseFactory.ok(response);
    }
}
